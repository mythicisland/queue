package net.mythicisland.queue.runtime.repository

import build.buf.gen.mythicisland.queue.v1.QueueStatus
import net.mythicisland.queue.shared.queue.Queue
import net.mythicisland.queue.shared.queue.QueueType
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.persistence.PersistenceQueueRepository
import net.mythicisland.queue.runtime.persistence.QueueTypeActivityRepository
import net.mythicisland.queue.runtime.reconciler.QueueStatusReconciler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for managing queues and player-to-queue mappings,
 * backed by a persistence layer for durability across restarts.
 */
class QueueRepository(
    private val types: QueueTypeRepository,
    private val persistence: PersistenceQueueRepository,
    private val activity: QueueTypeActivityRepository,
) {

    private val logger = LogManager.getLogger(QueueRepository::class.java)

    private val playersToQueue = ConcurrentHashMap<UUID, UUID>()
    private val queues = ConcurrentHashMap<UUID, Queue>()

    /** Protobuf snapshots of the last persisted queue state, used for change detection. */
    private val snapshots = ConcurrentHashMap<UUID, build.buf.gen.mythicisland.queue.v1.Queue>()

    /** Per-type mutexes to serialize enqueue operations for the same queue type. */
    private val typeMutexes = ConcurrentHashMap<String, Mutex>()

    private var reconciler: QueueStatusReconciler? = null
    private var publisher: EventPublisher? = null

    fun setReconciler(reconciler: QueueStatusReconciler) {
        this.reconciler = reconciler
    }

    fun setEventPublisher(publisher: EventPublisher) {
        this.publisher = publisher
    }

    /**
     * Loads all queues from the database into memory.
     * Queues in server-dependent states are reset to SEARCHING_SERVER.
     * Finished queues are deleted from the database.
     */
    fun loadFromDatabase() {
        val loaded = persistence.loadAll()
        var restored = 0
        var cleaned = 0

        for (queue in loaded) {
            when (queue.status) {
                QueueStatus.FINISHED -> {
                    persistence.delete(queue.id)
                    cleaned++
                    continue
                }
                QueueStatus.SEARCHING_SERVER,
                QueueStatus.WAITING_FOR_SERVER,
                QueueStatus.SERVER_READY,
                QueueStatus.COUNTDOWN,
                QueueStatus.TELEPORTING -> {
                    queue.status = QueueStatus.SEARCHING_SERVER
                    queue.countdownRemaining = 0
                }
                else -> {}
            }

            queues[queue.id] = queue
            snapshots[queue.id] = queue.toDefinition()
            queue.players.forEach { playersToQueue[it] = queue.id }
            persistence.save(queue)
            restored++
        }

        logger.info("Loaded {} queues from database ({} cleaned up)", restored, cleaned)
    }

    fun getQueueByPlayer(playerId: UUID): Queue? {
        return playersToQueue[playerId]?.let { queues[it] }
    }

    fun deleteQueue(queueId: UUID): Boolean {
        val queue = queues[queueId] ?: return false
        val removedPlayers = playersToQueue.filter { it.value == queueId }.keys
        queues.remove(queueId)
        snapshots.remove(queueId)
        removedPlayers.forEach { playersToQueue.remove(it) }
        reconciler?.clear(queueId)
        publisher?.publishQueueDeleted(queue)
        persistence.delete(queueId)
        logger.info("Deleted queue {} (removed {} player mappings)", queueId, removedPlayers.size)
        return true
    }

    /**
     * Enqueues players into a queue of the given type.
     *
     * Finds an existing queue with available capacity in NOT_ENOUGH_PLAYERS or
     * WAITING_COUNTDOWN status, or creates a new one if none fits.
     *
     * @param queueType The queue type name
     * @param playerIds The player UUIDs to enqueue
     * @return Success with the queue, or failure if the type doesn't exist or players are already queued
     */
    suspend fun enqueue(queueType: String, playerIds: List<UUID>): Result<Queue> {
        val type = types.find(queueType)
            ?: return Result.failure(NoSuchElementException("Queue type '$queueType' not found"))

        val mutex = typeMutexes.getOrPut(queueType) { Mutex() }
        return mutex.withLock {
            if (playerIds.any { playersToQueue.containsKey(it) }) {
                return@withLock Result.failure(IllegalStateException("Some players are already in a queue"))
            }

            val existingQueue = findQueue(queueType, playerIds.size)
            val queue = existingQueue ?: createQueue(type)

            if (existingQueue != null) {
                logger.info("Players {} joining existing queue {} (type={}, players={})", playerIds, queue.id, queue.type, queue.players.size)
            } else {
                logger.info("Players {} created new queue {} (type={}, capacity={})", playerIds, queue.id, queue.type, queue.capacity)
            }

            queue.players.addAll(playerIds)
            queues[queue.id] = queue
            playerIds.forEach { playersToQueue[it] = queue.id }
            persistence.save(queue)

            activity.record(queueType, playerIds)

            if (existingQueue == null) {
                publisher?.publishQueueCreated(queue)
            }
            publisher?.publishEnqueue(queue, playerIds)

            reconciler?.reconcile(queue.id)
            Result.success(queue)
        }
    }

    private fun createQueue(type: QueueType): Queue {
        val queue = Queue(
            id = UUID.randomUUID(),
            type = type.name,
            capacity = type.maxCapacity,
            players = mutableListOf(),
            status = QueueStatus.NOT_ENOUGH_PLAYERS,
        )
        queues[queue.id] = queue
        snapshots[queue.id] = queue.toDefinition()
        persistence.save(queue)
        return queue
    }

    /**
     * Removes a player from their current queue.
     *
     * @param playerId The player UUID to dequeue
     * @return true if the player was successfully removed
     */
    private suspend fun dequeue(playerId: UUID): Boolean {
        if (!playersToQueue.containsKey(playerId)) {
            logger.debug("Dequeue failed: player {} is not in any queue", playerId)
            return false
        }
        val queue = getQueueByPlayer(playerId) ?: return false
        if (!playersToQueue.remove(playerId, queue.id)) return false
        if (!queue.players.remove(playerId)) {
            playersToQueue[playerId] = queue.id
            return false
        }
        logger.info("Player {} left queue {} (type={}, remaining={})", playerId, queue.id, queue.type, queue.players.size)
        publisher?.publishDequeue(queue, listOf(playerId))
        persistence.save(queue)
        reconciler?.reconcile(queue.id)
        return true
    }

    /**
     * Removes multiple players from their queues.
     *
     * @param playerIds The player UUIDs to dequeue
     * @return true if all players were successfully removed
     */
    suspend fun dequeue(playerIds: List<UUID>): Boolean {
        return playerIds.all { dequeue(it) }
    }

    fun getAllQueues(): List<Queue> {
        return queues.values.toList()
    }

    fun getAllQueuesByType(type: String): List<Queue> {
        return queues.values.filter { it.type == type }
    }

    fun getQueue(queueId: UUID): Queue? {
        return queues[queueId]
    }

    fun updateQueue(queue: Queue) {
        if (!queues.containsKey(queue.id)) return

        val before = snapshots[queue.id]
        val after = queue.toDefinition()
        queues[queue.id] = queue
        snapshots[queue.id] = after
        persistence.save(queue)

        if (before != null && before != after) {
            publisher?.publishQueueUpdated(before, after)
        }
    }

    private fun findQueue(queueType: String, playerAmount: Int): Queue? {
        return queues.values.firstOrNull {
            it.type == queueType
                && (it.status == QueueStatus.NOT_ENOUGH_PLAYERS || it.status == QueueStatus.WAITING_COUNTDOWN)
                && playerAmount + it.players.size <= it.capacity
        }
    }

}
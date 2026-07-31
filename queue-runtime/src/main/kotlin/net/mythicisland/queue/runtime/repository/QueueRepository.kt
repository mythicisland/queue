package net.mythicisland.queue.runtime.repository

import build.buf.gen.mythicisland.queue.v1.QueueStatus
import net.mythicisland.queue.shared.queue.Queue
import net.mythicisland.queue.shared.queue.QueueType
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.reconciler.QueueReconciler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for managing active queues.
 *
 * @param types the repository for queue types.
 * @param publisher the publisher to publish events to NATS.
 */
class QueueRepository(
    private val types: QueueTypeRepository,
    private val publisher: EventPublisher
) {

    private val logger = LogManager.getLogger(QueueRepository::class.java)

    private val playersToQueue = ConcurrentHashMap<UUID, UUID>()
    private val queues = ConcurrentHashMap<UUID, Queue>()
    private val snapshots = ConcurrentHashMap<UUID, build.buf.gen.mythicisland.queue.v1.Queue>()
    private val typeMutex = ConcurrentHashMap<String, Mutex>()

    private var reconciler: QueueReconciler? = null

    /**
     * Sets the [QueueReconciler] to reconcile queues.
     *
     * @param reconciler the reconciler to set.
     */
    fun setReconciler(reconciler: QueueReconciler) {
        this.reconciler = reconciler
    }

    /**
     * Gets a queue by its id.
     *
     * @param queueId the UUID of the queue.
     * @return the queue, or null if no queue with that id exists.
     */
    fun getQueue(queueId: UUID): Queue? {
        return queues[queueId]
    }

    /**
     * Gets the queue a player is in.
     *
     * @param id the UUID of the player.
     * @return the queue the player is in, or null if the player is not queued.
     */
    fun getQueueByPlayer(id: UUID): Queue? {
        return playersToQueue[id]?.let { queues[it] }
    }

    /**
     * Gets all active queues.
     *
     * @return all active queues.
     */
    fun getAllQueues(): List<Queue> {
        return queues.values.toList()
    }

    /**
     * Gets all active queues of a type.
     *
     * @param type the name of the queue type.
     * @return all active queues of that type.
     */
    fun getAllQueuesByType(type: String): List<Queue> {
        return queues.values.filter { it.type == type }
    }

    /**
     * Finds a queue of a type that is still waiting and has room for more players.
     *
     * @param queueType the name of the queue type.
     * @param playerAmount the amount of players that want to join.
     * @return a matching queue, or null if none has enough room.
     */
    private fun findQueue(queueType: String, playerAmount: Int): Queue? {
        return queues.values.firstOrNull {
            it.type == queueType
                && (it.status == QueueStatus.NOT_ENOUGH_PLAYERS || it.status == QueueStatus.WAITING_COUNTDOWN)
                && playerAmount + it.players.size <= it.capacity
        }
    }

    /**
     * Enqueues a single player or a group of players.
     *
     * @param queueType the name of the queue type to enqueue into.
     * @param playerIds the UUIDs of the players to enqueue.
     * @return a result with the queue the players joined.
     */
    suspend fun enqueue(queueType: String, playerIds: List<UUID>): Result<Queue> {
        val type = types.find(queueType)
            ?: return Result.failure(NoSuchElementException("Queue type '$queueType' not found"))

        val mutex = typeMutex.getOrPut(queueType) { Mutex() }
        return mutex.withLock {
            if (playerIds.any { playersToQueue.containsKey(it) }) {
                return@withLock Result.failure(IllegalStateException("Some players are already in a queue"))
            }

            val existingQueue = findQueue(queueType, playerIds.size)
            val queue = existingQueue ?: createQueue(type)

            if (existingQueue != null) {
                logger.info("Players {} joining existing queue {} (type={}, players={})", playerIds, queue.id, queue.type, queue.players.size)
            } else {
                logger.info("Players {} created new queue {} (type={}, capacity={})", playerIds, queue.id, queue.type, queue.players.size)
            }

            queue.players.addAll(playerIds)
            queues[queue.id] = queue
            playerIds.forEach { playersToQueue[it] = queue.id }

            if (existingQueue == null) {
                publisher.publishQueueCreated(queue)
            }
            publisher.publishEnqueue(queue, playerIds)

            reconciler?.reconcile(queue.id)
            Result.success(queue)
        }
    }

    /**
     * Dequeues a single player or a group of players.
     *
     * @param playerIds the UUIDs of the players to dequeue.
     * @return true if every player was successfully dequeued.
     */
    suspend fun dequeue(playerIds: List<UUID>): Boolean {
        return playerIds.all { dequeue(it) }
    }

    /**
     * Dequeues a player.
     *
     * @param playerId the UUID of the player to dequeue.
     * @return true if the player was successfully dequeued.
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
        publisher.publishDequeue(queue, listOf(playerId))
        reconciler?.reconcile(queue.id)
        return true
    }

    /**
     * Creates a new queue of a type.
     *
     * @param type the queue type to create the queue for.
     * @return the created queue.
     */
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
        return queue
    }

    /**
     * Updates a queue and publishes an update event if anything changed.
     *
     * @param queue the queue to update.
     */
    fun updateQueue(queue: Queue) {
        if (!queues.containsKey(queue.id)) return

        val before = snapshots[queue.id]
        val after = queue.toDefinition()
        queues[queue.id] = queue
        snapshots[queue.id] = after

        if (before != null && before != after) {
            publisher.publishQueueUpdated(before, after)
        }
    }

    /**
     * Deletes a queue.
     *
     * @param queueId the UUID of the queue to delete.
     * @return true if the queue was successfully deleted.
     */
    fun deleteQueue(queueId: UUID): Boolean {
        val queue = queues[queueId] ?: return false
        queues.remove(queueId)
        snapshots.remove(queueId)
        var removedCount = 0
        playersToQueue.entries.removeAll { (_, id) ->
            (id == queueId).also { if (it) removedCount++ }
        }
        reconciler?.clear(queueId)
        publisher.publishQueueDeleted(queue)
        logger.info("Successfully deleted queue $queueId")
        return true
    }

}

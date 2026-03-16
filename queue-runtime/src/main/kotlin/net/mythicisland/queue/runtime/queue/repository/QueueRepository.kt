package net.mythicisland.queue.runtime.queue.repository

import build.buf.gen.mythicisland.queue.v1.QueueStatus
import net.mythicisland.queue.runtime.queue.Queue
import net.mythicisland.queue.runtime.queue.QueueType
import net.mythicisland.queue.runtime.queue.event.EventPublisher
import net.mythicisland.queue.runtime.queue.reconciler.QueueStatusReconciler
import org.apache.logging.log4j.LogManager
import java.util.UUID

/**
 * In-memory repository for managing queues and player-to-queue mappings.
 *
 * @property types Repository for queue type configurations
 */
class QueueRepository(
    private val types: QueueTypeRepository,
) {

    private val logger = LogManager.getLogger(QueueRepository::class.java)

    private val playersToQueue = mutableMapOf<UUID, UUID>()
    private val queues = mutableMapOf<UUID, Queue>()

    /** Protobuf snapshots of the last persisted queue state, used for change detection. */
    private val snapshots = mutableMapOf<UUID, build.buf.gen.mythicisland.queue.v1.Queue>()

    private lateinit var reconciler: QueueStatusReconciler
    private lateinit var eventPublisher: EventPublisher

    fun setReconciler(reconciler: QueueStatusReconciler) {
        this.reconciler = reconciler
    }

    fun setEventPublisher(eventPublisher: EventPublisher) {
        this.eventPublisher = eventPublisher
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
        reconciler.clear(queueId)
        eventPublisher.publishQueueDeleted(queue)
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

        if (playerIds.any { playersToQueue.containsKey(it) }) {
            return Result.failure(IllegalStateException("Some players are already in a queue"))
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

        if (existingQueue == null) {
            eventPublisher.publishQueueCreated(queue)
        }
        eventPublisher.publishEnqueue(queue, playerIds)

        reconciler.reconcile(queue.id)
        return Result.success(queue)
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
        eventPublisher.publishDequeue(queue, listOf(playerId))
        reconciler.reconcile(queue.id)
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

        if (before != null && before != after) {
            eventPublisher.publishQueueUpdated(before, after)
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

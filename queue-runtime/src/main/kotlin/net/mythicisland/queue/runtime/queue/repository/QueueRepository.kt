package net.mythicisland.queue.runtime.queue.repository

import build.buf.gen.mythicisland.queue.v1.QueueStatus
import net.mythicisland.queue.runtime.queue.Queue
import net.mythicisland.queue.runtime.queue.QueueType
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

    private lateinit var reconciler: QueueStatusReconciler

    fun setReconciler(reconciler: QueueStatusReconciler) {
        this.reconciler = reconciler
    }

    fun getQueueByPlayer(playerId: UUID): Queue? {
        return playersToQueue[playerId]?.let { queues[it] }
    }

    fun deleteQueue(queueId: UUID): Boolean {
        if (!queues.containsKey(queueId)) return false
        queues.remove(queueId)
        playersToQueue.filter { it.value == queueId }.forEach { playersToQueue.remove(it.key) }
        reconciler.clear(queueId)
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

        val queue = findQueue(queueType, playerIds.size) ?: createQueue(type)
        queue.players.addAll(playerIds)
        queues[queue.id] = queue
        playerIds.forEach { playersToQueue[it] = queue.id }
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
        return queue
    }

    /**
     * Removes a player from their current queue.
     *
     * @param playerId The player UUID to dequeue
     * @return true if the player was successfully removed
     */
    suspend fun dequeue(playerId: UUID): Boolean {
        if (!playersToQueue.containsKey(playerId)) return false
        val queue = getQueueByPlayer(playerId) ?: return false
        if (!playersToQueue.remove(playerId, queue.id)) return false
        if (!queue.players.remove(playerId)) {
            playersToQueue[playerId] = queue.id
            return false
        }
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
        queues[queue.id] = queue
    }

    private fun findQueue(queueType: String, playerAmount: Int): Queue? {
        return queues.values.firstOrNull {
            it.type == queueType
                && (it.status == QueueStatus.NOT_ENOUGH_PLAYERS || it.status == QueueStatus.WAITING_COUNTDOWN)
                && playerAmount + it.players.size <= it.capacity
        }
    }

}

package net.mythicisland.queue.runtime.queue.repository

import app.simplecloud.api.player.PlayerApi
import build.buf.gen.mythicisland.queue.v1.QueueStatus
import io.grpc.Status
import net.kyori.adventure.text.Component
import net.mythicisland.queue.runtime.extension.asPlayer
import net.mythicisland.queue.runtime.queue.Queue
import net.mythicisland.queue.runtime.queue.QueueType
import net.mythicisland.queue.runtime.queue.reconciler.QueueStatusReconciler
import org.apache.logging.log4j.LogManager
import java.util.UUID

class QueueRepository(
    private val types: QueueTypeRepository,
    private val api: PlayerApi
) {

    private val logger = LogManager.getLogger(QueueRepository::class.java)

    private val playersToQueue = mutableMapOf<UUID, UUID>()
    private val queues = mutableMapOf<UUID, Queue>()

    private lateinit var reconciler: QueueStatusReconciler

    fun setReconciler(reconciler: QueueStatusReconciler) {
        this.reconciler = reconciler
    }

    fun getQueueByPlayer(playerId: UUID): Queue? {
        return playersToQueue.firstNotNullOfOrNull { if (it.key == playerId) it.value else null }?.let { queues[it] }
    }

    fun deleteQueue(queueId: UUID): Boolean {
        if (!queues.containsKey(queueId)) return false
        queues.remove(queueId)
        playersToQueue.filter { it.value == queueId }.forEach { playersToQueue.remove(it.key) }
        reconciler.clear(queueId)
        return true
    }

    /* suspend fun enqueue(queueType: String, playerIds: List<UUID>): Queue {
        val type = types.find(queueType) ?: run {
            playerIds.forEach { uuid ->
                val player = uuid.asPlayer(api)
                player.sendMessage((Component.text("There's no queue with the name $queueType.")))
            }

            throw Status.NOT_FOUND.withDescription("Failed to enqueue: Cannot find queue $queueType").asRuntimeException()
        }

        if (playerIds.any { playersToQueue.containsKey(it) }) {
            playerIds.forEach { uuid ->
                val player = uuid.asPlayer(api)
                player.sendMessage((Component.text("Some of the players you were enqueued with are already in a queue.")))
            }

            throw Status.FAILED_PRECONDITION.withDescription("Failed to enqueue: Some players are already in a queue").asRuntimeException()
        }

        val queue = findQueue(queueType, playerIds.size) ?: createQueue(type)
        queue.players.addAll(playerIds)
        queues[queue.id] = queue
        playerIds.forEach { playersToQueue[it] = queue.id }
        reconciler.reconcile(queue.id)
        return queue
    } */

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

    suspend fun dequeue(playerIds: List<UUID>): Boolean {
        return !playerIds.any { !dequeue(it) }
    }

    fun getAllQueues(): List<Queue> {
        return queues.values.toList()
    }

    fun getAllQueuesByType(type: String): List<Queue> {
        return queues.values.filter { it.type == type }.toList()
    }


    fun getQueue(queueId: UUID): Queue? {
        return queues[queueId]
    }

    fun updateQueue(queue: Queue) {
        if (!queues.containsKey(queue.id)) return
        queues[queue.id] = queue
    }

    private fun findQueue(queueType: String, playerAmount: Int): Queue? {
        return queues.values.firstOrNull { it.type == queueType && playerAmount + it.players.size <= it.capacity }
    }


}
package net.mythicisland.queue.runtime.service

import build.buf.gen.mythicisland.queue.v1.*
import io.grpc.Status
import net.mythicisland.queue.shared.extension.asUUID
import net.mythicisland.queue.shared.message.CommandMessages
import net.mythicisland.queue.shared.message.PlayerMessenger
import net.mythicisland.queue.runtime.repository.QueueRepository
import org.apache.logging.log4j.LogManager

/**
 * gRPC service for queue operations.
 */
class QueueService(
    private val queues: QueueRepository,
    private val messenger: PlayerMessenger,
) : QueueServiceGrpcKt.QueueServiceCoroutineImplBase() {

    private val logger = LogManager.getLogger(QueueService::class.java)

    /**
     * Enqueues players into a queue of the specified type.
     *
     * @param request The enqueue request containing the queue type and player IDs
     * @return The enqueue response containing the queue the players were added to
     * @throws io.grpc.StatusException if the queue type is not found or players are already queued
     */
    override suspend fun enqueue(request: EnqueueRequest): EnqueueResponse {
        val playerIds = request.playerIdsList.map { it.asUUID() }
        logger.info("Enqueue request: type={}, players={}", request.type, playerIds)

        val result = queues.enqueue(request.type, playerIds)

        val queue = result.getOrElse { error ->
            logger.warn("Enqueue failed for players {} in type '{}': {}", playerIds, request.type, error.message)
            val (status, message) = when (error) {
                is NoSuchElementException -> Status.NOT_FOUND to CommandMessages.ENQUEUE_NOT_FOUND
                is IllegalStateException -> Status.FAILED_PRECONDITION to CommandMessages.ENQUEUE_ALREADY_QUEUED
                else -> Status.INTERNAL to CommandMessages.ENQUEUE_FAILED
            }
            messenger.send(playerIds, message)
            throw status.withDescription(error.message).asRuntimeException()
        }

        logger.info("Enqueue success: players {} joined queue {} (type={}, players={})", playerIds, queue.id, queue.type, queue.players.size)
        messenger.send(playerIds, CommandMessages.ENQUEUE_SUCCESS)
        return enqueueResponse { this.queue = queue.toDefinition() }
    }

    /**
     * Dequeues players from their current queues.
     *
     * @param request The dequeue request containing the player IDs to remove
     * @return The dequeue response
     * @throws io.grpc.StatusException if any player could not be dequeued
     */
    override suspend fun dequeue(request: DequeueRequest): DequeueResponse {
        val playerIds = request.playerIdsList.map { it.asUUID() }
        logger.info("Dequeue request: players={}", playerIds)

        val success = queues.dequeue(playerIds)

        if (!success) {
            logger.warn("Dequeue failed: some players {} could not be dequeued", playerIds)
            messenger.send(playerIds, CommandMessages.DEQUEUE_NOT_IN_QUEUE)
            throw Status.NOT_FOUND
                .withDescription("Some players could not be dequeued")
                .asRuntimeException()
        }

        logger.info("Dequeue success: players {} removed from their queues", playerIds)
        messenger.send(playerIds, CommandMessages.DEQUEUE_SUCCESS)
        return dequeueResponse { }
    }

}
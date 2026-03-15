package net.mythicisland.queue.runtime.queue.service

import build.buf.gen.mythicisland.queue.v1.*
import io.grpc.Status
import net.mythicisland.queue.runtime.extension.asUUID
import net.mythicisland.queue.runtime.queue.repository.QueueRepository
import org.apache.logging.log4j.LogManager

/**
 * gRPC service for queue operations.
 *
 * @property queues The queue repository for data access
 */
class QueueService(
    private val queues: QueueRepository
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
            when (error) {
                is NoSuchElementException -> throw Status.NOT_FOUND
                    .withDescription(error.message)
                    .asRuntimeException()
                is IllegalStateException -> throw Status.FAILED_PRECONDITION
                    .withDescription(error.message)
                    .asRuntimeException()
                else -> throw Status.INTERNAL
                    .withDescription("Unexpected error during enqueue")
                    .asRuntimeException()
            }
        }

        logger.info("Enqueue success: players {} joined queue {} (type={}, players={})", playerIds, queue.id, queue.type, queue.players.size)
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
            throw Status.NOT_FOUND
                .withDescription("Some players could not be dequeued")
                .asRuntimeException()
        }

        logger.info("Dequeue success: players {} removed from their queues", playerIds)
        return dequeueResponse { }
    }

}

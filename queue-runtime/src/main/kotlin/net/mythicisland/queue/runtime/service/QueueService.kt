package net.mythicisland.queue.runtime.service

import build.buf.gen.mythicisland.queue.v1.*
import io.grpc.Status
import net.mythicisland.queue.shared.extension.asUUID
import net.mythicisland.queue.runtime.repository.QueueRepository
import org.apache.logging.log4j.LogManager

/**
 * gRPC service for queue operations.
 */
class QueueService(
    private val queues: QueueRepository,
) : QueueServiceGrpcKt.QueueServiceCoroutineImplBase() {

    private val logger = LogManager.getLogger(QueueService::class.java)

    override suspend fun enqueue(request: EnqueueRequest): EnqueueResponse {
        val playerIds = request.playerIdsList.map { it.asUUID() }
        logger.info("Enqueue request: type={}, players={}", request.type, playerIds)

        val result = queues.enqueue(request.type, playerIds)

        val queue = result.getOrElse { error ->
            logger.warn("Enqueue failed for players {} in type '{}': {}", playerIds, request.type, error.message)
            val status = when (error) {
                is NoSuchElementException -> Status.NOT_FOUND
                is IllegalStateException -> Status.FAILED_PRECONDITION
                else -> Status.INTERNAL
            }
            throw status.withDescription(error.message).asRuntimeException()
        }

        logger.info("Enqueue success: players {} joined queue {} (type={}, players={})", playerIds, queue.id, queue.type, queue.players.size)
        return enqueueResponse { this.queue = queue.toDefinition() }
    }

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
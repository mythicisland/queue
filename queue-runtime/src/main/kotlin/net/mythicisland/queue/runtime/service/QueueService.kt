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
        val players = request.playerIdsList.map { it.asUUID() }
        val result = queues.enqueue(request.type, players)
        val queue = result.getOrElse { error ->
            logger.warn("Enqueue failed for players {} in type '{}': {}", players, request.type, error.message)
            val status = when (error) {
                is NoSuchElementException -> Status.NOT_FOUND
                is IllegalStateException -> Status.FAILED_PRECONDITION
                else -> Status.INTERNAL
            }
            throw status.withDescription(error.message).asRuntimeException()
        }

        return enqueueResponse { this.queue = queue.toDefinition() }
    }

    override suspend fun dequeue(request: DequeueRequest): DequeueResponse {
        val playerIds = request.playerIdsList.map { it.asUUID() }
        val success = queues.dequeue(playerIds)

        if (!success) {
            logger.warn("Dequeue failed: some players {} could not be dequeued", playerIds)
            throw Status.NOT_FOUND
                .withDescription("Some players could not be dequeued")
                .asRuntimeException()
        }

        return dequeueResponse { }
    }

}
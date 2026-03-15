package net.mythicisland.queue.runtime.queue.service

import build.buf.gen.mythicisland.queue.v1.*
import io.grpc.Status
import net.mythicisland.queue.runtime.extension.asUUID
import net.mythicisland.queue.runtime.queue.QueueType
import net.mythicisland.queue.runtime.queue.repository.QueueRepository
import net.mythicisland.queue.runtime.queue.repository.QueueTypeRepository
import org.apache.logging.log4j.LogManager

/**
 * gRPC service for queue data queries.
 *
 * Provides read access to queues, queue types, and player positions
 * for use in GUIs, tab completion, scoreboards, etc.
 *
 * @property queues The queue repository for queue data access
 * @property types The queue type repository for type configuration access
 */
class QueueDataService(
    private val queues: QueueRepository,
    private val types: QueueTypeRepository,
) : QueueDataServiceGrpcKt.QueueDataServiceCoroutineImplBase() {

    private val logger = LogManager.getLogger(QueueDataService::class.java)

    /**
     * Gets a single queue by its ID.
     *
     * @param request The request containing the queue ID
     * @return The response containing the queue
     * @throws io.grpc.StatusException NOT_FOUND if the queue does not exist
     */
    override suspend fun getQueue(request: GetQueueRequest): GetQueueResponse {
        val queueId = request.queueId.asUUID()

        val queue = queues.getQueue(queueId)
            ?: throw Status.NOT_FOUND
                .withDescription("Queue '$queueId' not found")
                .asRuntimeException()

        return getQueueResponse { this.queue = queue.toDefinition() }
    }

    /**
     * Gets all active queues.
     *
     * @return The response containing all queues
     */
    override suspend fun getAllQueues(request: GetAllQueuesRequest): GetAllQueuesResponse {
        val allQueues = queues.getAllQueues()

        return getAllQueuesResponse {
            this.queues.addAll(allQueues.map { it.toDefinition() })
        }
    }

    /**
     * Gets all active queues of a specific type.
     *
     * @param request The request containing the queue type name
     * @return The response containing all matching queues
     */
    override suspend fun getQueuesByType(request: GetQueuesByTypeRequest): GetQueuesByTypeResponse {
        val matchingQueues = queues.getAllQueuesByType(request.type)

        return getQueuesByTypeResponse {
            this.queues.addAll(matchingQueues.map { it.toDefinition() })
        }
    }

    /**
     * Gets the queue a player is currently in.
     *
     * @param request The request containing the player UUID
     * @return The response containing the player's queue
     * @throws io.grpc.StatusException NOT_FOUND if the player is not in any queue
     */
    override suspend fun getQueueByPlayer(request: GetQueueByPlayerRequest): GetQueueByPlayerResponse {
        val playerId = request.playerId.asUUID()

        val queue = queues.getQueueByPlayer(playerId)
            ?: throw Status.NOT_FOUND
                .withDescription("Player '$playerId' is not in any queue")
                .asRuntimeException()

        return getQueueByPlayerResponse { this.queue = queue.toDefinition() }
    }

    /**
     * Gets a player's position in their current queue.
     *
     * @param request The request containing the player UUID
     * @return The response containing the queue and the player's 1-based position
     * @throws io.grpc.StatusException NOT_FOUND if the player is not in any queue
     */
    override suspend fun getPlayerPosition(request: GetPlayerPositionRequest): GetPlayerPositionResponse {
        val playerId = request.playerId.asUUID()

        val queue = queues.getQueueByPlayer(playerId)
            ?: throw Status.NOT_FOUND
                .withDescription("Player '$playerId' is not in any queue")
                .asRuntimeException()

        val position = queue.players.indexOf(playerId) + 1

        return getPlayerPositionResponse {
            this.queue = queue.toDefinition()
            this.position = position
        }
    }

    /**
     * Gets a single queue type by its name.
     *
     * @param request The request containing the queue type name
     * @return The response containing the queue type configuration
     * @throws io.grpc.StatusException NOT_FOUND if the queue type does not exist
     */
    override suspend fun getQueueType(request: GetQueueTypeRequest): GetQueueTypeResponse {
        val type = types.find(request.name)
            ?: throw Status.NOT_FOUND
                .withDescription("Queue type '${request.name}' not found")
                .asRuntimeException()

        return getQueueTypeResponse { this.queueType = type.toDefinition() }
    }

    /**
     * Gets all available queue types.
     *
     * @return The response containing all queue type configurations
     */
    override suspend fun getAllQueueTypes(request: GetAllQueueTypesRequest): GetAllQueueTypesResponse {
        val allTypes = types.getAll()

        return getAllQueueTypesResponse {
            this.queueTypes.addAll(allTypes.map(QueueType::toDefinition))
        }
    }

}

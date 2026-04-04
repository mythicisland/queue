package net.mythicisland.queue.runtime.service

import build.buf.gen.mythicisland.queue.v1.*
import io.grpc.Status
import net.mythicisland.queue.shared.extension.asUUID
import net.mythicisland.queue.shared.queue.QueueType
import net.mythicisland.queue.runtime.rating.QueueTypeRatingCalculator
import net.mythicisland.queue.runtime.repository.QueueRepository
import net.mythicisland.queue.runtime.repository.QueueTypeRepository
import org.apache.logging.log4j.LogManager

/**
 * gRPC service for queue data queries.
 */
class QueueDataService(
    private val queues: QueueRepository,
    private val types: QueueTypeRepository,
    private val ratingCalculator: QueueTypeRatingCalculator,
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
        logger.debug("GetQueue request: queueId={}", queueId)

        val queue = queues.getQueue(queueId)
            ?: throw Status.NOT_FOUND
                .withDescription("Queue '$queueId' not found")
                .asRuntimeException()

        logger.debug("GetQueue response: queue={} type={} status={} players={}", queueId, queue.type, queue.status, queue.players.size)
        return getQueueResponse { this.queue = queue.toDefinition() }
    }

    /**
     * Gets all active queues.
     *
     * @return The response containing all queues
     */
    override suspend fun getAllQueues(request: GetAllQueuesRequest): GetAllQueuesResponse {
        val allQueues = queues.getAllQueues()
        logger.debug("GetAllQueues response: {} queues", allQueues.size)

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
        logger.debug("GetQueuesByType request: type={}", request.type)
        val matchingQueues = queues.getAllQueuesByType(request.type)
        logger.debug("GetQueuesByType response: {} queues for type '{}'", matchingQueues.size, request.type)

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
        logger.debug("GetQueueByPlayer request: playerId={}", playerId)

        val queue = queues.getQueueByPlayer(playerId)
            ?: throw Status.NOT_FOUND
                .withDescription("Player '$playerId' is not in any queue")
                .asRuntimeException()

        logger.debug("GetQueueByPlayer response: player {} is in queue {} (type={}, status={})", playerId, queue.id, queue.type, queue.status)
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
        logger.debug("GetPlayerPosition request: playerId={}", playerId)

        val queue = queues.getQueueByPlayer(playerId)
            ?: throw Status.NOT_FOUND
                .withDescription("Player '$playerId' is not in any queue")
                .asRuntimeException()

        val position = queue.players.indexOf(playerId) + 1
        logger.debug("GetPlayerPosition response: player {} is at position {} in queue {} ({} players total)", playerId, position, queue.id, queue.players.size)

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
        logger.debug("GetQueueType request: name={}", request.name)

        val type = types.find(request.name)
            ?: throw Status.NOT_FOUND
                .withDescription("Queue type '${request.name}' not found")
                .asRuntimeException()

        logger.debug("GetQueueType response: type={} group={} capacity={}-{}", type.name, type.group, type.minCapacity, type.maxCapacity)
        return getQueueTypeResponse { this.queueType = type.toDefinition() }
    }

    /**
     * Gets all available queue types.
     *
     * @return The response containing all queue type configurations
     */
    override suspend fun getAllQueueTypes(request: GetAllQueueTypesRequest): GetAllQueueTypesResponse {
        val allTypes = types.getAll()
        logger.debug("GetAllQueueTypes response: {} types", allTypes.size)

        return getAllQueueTypesResponse {
            this.queueTypes.addAll(allTypes.map(QueueType::toDefinition))
        }
    }

    /**
     * Gets the rating and activity stats for a single queue type.
     *
     * @param request The request containing the queue type name
     * @return The response containing the computed stats
     * @throws io.grpc.StatusException NOT_FOUND if the queue type does not exist
     */
    override suspend fun getQueueTypeStats(request: GetQueueTypeStatsRequest): GetQueueTypeStatsResponse {
        logger.debug("GetQueueTypeStats request: name={}", request.name)

        val stats = ratingCalculator.calculate(request.name)
            ?: throw Status.NOT_FOUND
                .withDescription("Queue type '${request.name}' not found")
                .asRuntimeException()

        logger.debug("GetQueueTypeStats response: type={} rating={} share={}%", request.name, stats.rating, stats.sharePercent)
        return getQueueTypeStatsResponse { this.stats = stats }
    }

    /**
     * Gets the rating and activity stats for all registered queue types.
     *
     * @return The response containing stats for every registered queue type
     */
    override suspend fun getAllQueueTypeStats(request: GetAllQueueTypeStatsRequest): GetAllQueueTypeStatsResponse {
        val allStats = ratingCalculator.calculateAll()
        logger.debug("GetAllQueueTypeStats response: {} types", allStats.size)

        return getAllQueueTypeStatsResponse {
            this.stats.addAll(allStats)
        }
    }
}

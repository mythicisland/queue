package net.mythicisland.queue.api.extensions

import build.buf.gen.mythicisland.queue.v1.*
import kotlinx.coroutines.future.await
import net.mythicisland.queue.api.data.QueueDataApi
import java.util.UUID

/**
 * Gets a single queue by its ID, suspending until the response is available.
 *
 * @param queueId the unique ID of the queue
 * @return the queue response
 * @throws io.grpc.StatusRuntimeException with `NOT_FOUND` if the queue doesn't exist
 */
suspend fun QueueDataApi.getQueueSuspending(queueId: UUID): GetQueueResponse {
    return getQueue(queueId).await()
}

/**
 * Gets all active queues, suspending until the response is available.
 *
 * @return the response containing all active queues
 */
suspend fun QueueDataApi.getAllQueuesSuspending(): GetAllQueuesResponse {
    return getAllQueues().await()
}

/**
 * Gets all active queues of a specific type, suspending until the response is available.
 *
 * @param type the queue type name
 * @return the response containing matching queues
 */
suspend fun QueueDataApi.getQueuesByTypeSuspending(type: String): GetQueuesByTypeResponse {
    return getQueuesByType(type).await()
}

/**
 * Gets the queue a player is currently in, suspending until the response is available.
 *
 * @param playerId the UUID of the player
 * @return the response containing the player's queue
 * @throws io.grpc.StatusRuntimeException with `NOT_FOUND` if the player is not in a queue
 */
suspend fun QueueDataApi.getQueueByPlayerSuspending(playerId: UUID): GetQueueByPlayerResponse {
    return getQueueByPlayer(playerId).await()
}

/**
 * Gets a player's 1-based position in their current queue, suspending until the response is available.
 *
 * @param playerId the UUID of the player
 * @return the response containing the queue and position
 * @throws io.grpc.StatusRuntimeException with `NOT_FOUND` if the player is not in a queue
 */
suspend fun QueueDataApi.getPlayerPositionSuspending(playerId: UUID): GetPlayerPositionResponse {
    return getPlayerPosition(playerId).await()
}

/**
 * Gets a single queue type configuration by name, suspending until the response is available.
 *
 * @param name the queue type name
 * @return the response containing the queue type
 * @throws io.grpc.StatusRuntimeException with `NOT_FOUND` if the type doesn't exist
 */
suspend fun QueueDataApi.getQueueTypeSuspending(name: String): GetQueueTypeResponse {
    return getQueueType(name).await()
}

/**
 * Gets all available queue type configurations, suspending until the response is available.
 *
 * @return the response containing all queue types
 */
suspend fun QueueDataApi.getAllQueueTypesSuspending(): GetAllQueueTypesResponse {
    return getAllQueueTypes().await()
}

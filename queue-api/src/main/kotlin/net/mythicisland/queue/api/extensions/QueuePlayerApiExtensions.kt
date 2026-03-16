package net.mythicisland.queue.api.extensions

import build.buf.gen.mythicisland.queue.v1.DequeueResponse
import build.buf.gen.mythicisland.queue.v1.EnqueueResponse
import kotlinx.coroutines.future.await
import net.mythicisland.queue.api.player.QueuePlayerApi
import java.util.UUID

/**
 * Enqueues multiple players into a queue, suspending until the operation completes.
 *
 * ```kotlin
 * val response = api.player().enqueueSuspending("minekart", listOf(player1Id, player2Id))
 * ```
 *
 * @param type the queue type name
 * @param playerIds the UUIDs of the players to enqueue
 * @return the enqueue response
 * @throws io.grpc.StatusRuntimeException on gRPC errors
 */
suspend fun QueuePlayerApi.enqueueSuspending(type: String, playerIds: List<UUID>): EnqueueResponse {
    return enqueue(type, playerIds).await()
}

/**
 * Enqueues a single player into a queue, suspending until the operation completes.
 *
 * ```kotlin
 * val response = api.player().enqueueSuspending("minekart", playerId)
 * ```
 *
 * @param type the queue type name
 * @param playerId the UUID of the player to enqueue
 * @return the enqueue response
 * @throws io.grpc.StatusRuntimeException on gRPC errors
 */
suspend fun QueuePlayerApi.enqueueSuspending(type: String, playerId: UUID): EnqueueResponse {
    return enqueue(type, playerId).await()
}

/**
 * Dequeues multiple players from their current queues, suspending until the operation completes.
 *
 * ```kotlin
 * val response = api.player().dequeueSuspending(listOf(player1Id, player2Id))
 * ```
 *
 * @param playerIds the UUIDs of the players to dequeue
 * @return the dequeue response
 * @throws io.grpc.StatusRuntimeException on gRPC errors
 */
suspend fun QueuePlayerApi.dequeueSuspending(playerIds: List<UUID>): DequeueResponse {
    return dequeue(playerIds).await()
}

/**
 * Dequeues a single player from their current queue, suspending until the operation completes.
 *
 * ```kotlin
 * val response = api.player().dequeueSuspending(playerId)
 * ```
 *
 * @param playerId the UUID of the player to dequeue
 * @return the dequeue response
 * @throws io.grpc.StatusRuntimeException on gRPC errors
 */
suspend fun QueuePlayerApi.dequeueSuspending(playerId: UUID): DequeueResponse {
    return dequeue(playerId).await()
}

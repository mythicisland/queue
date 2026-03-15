package net.mythicisland.queue.api.player;

import build.buf.gen.mythicisland.queue.v1.EnqueueResponse;
import build.buf.gen.mythicisland.queue.v1.DequeueResponse;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * API for queue player operations.
 */
public interface QueuePlayerApi {

    /**
     * Enqueues players into a queue of the specified type.
     *
     * @param type      the queue type name (e.g. "minekart")
     * @param playerIds the UUIDs of the players to enqueue
     * @return a future completing with the enqueue response
     */
    CompletableFuture<EnqueueResponse> enqueue(String type, List<UUID> playerIds);

    /**
     * Enqueues a single player into a queue of the specified type.
     *
     * @param type     the queue type name
     * @param playerId the UUID of the player to enqueue
     * @return a future completing with the enqueue response
     */
    default CompletableFuture<EnqueueResponse> enqueue(String type, UUID playerId) {
        return enqueue(type, List.of(playerId));
    }

    /**
     * Dequeues players from their current queues.
     *
     * @param playerIds the UUIDs of the players to dequeue
     * @return a future completing with the dequeue response
     */
    CompletableFuture<DequeueResponse> dequeue(List<UUID> playerIds);

    /**
     * Dequeues a single player from their current queue.
     *
     * @param playerId the UUID of the player to dequeue
     * @return a future completing with the dequeue response
     */
    default CompletableFuture<DequeueResponse> dequeue(UUID playerId) {
        return dequeue(List.of(playerId));
    }
}

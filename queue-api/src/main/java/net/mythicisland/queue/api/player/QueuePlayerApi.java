package net.mythicisland.queue.api.player;

import build.buf.gen.mythicisland.queue.v1.EnqueueResponse;
import build.buf.gen.mythicisland.queue.v1.DequeueResponse;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * API for performing player related queuing operations.
 */
public interface QueuePlayerApi {

    /**
     * Enqueues a group of players into a queue of the specified type.
     *
     * @param type the name of the queue type to join
     * @param playerIds a list of UUIDs of the players to enqueue together
     * @return a future completing with the result of the enqueue operation
     */
    CompletableFuture<EnqueueResponse> enqueue(String type, List<UUID> playerIds);

    /**
     * Enqueues a player into a queue of the specified type.
     *
     * @param type the name of the queue type to join
     * @param playerId the UUID of the player to enqueue
     * @return a future completing with the result of the enqueue operation
     */
    default CompletableFuture<EnqueueResponse> enqueue(String type, UUID playerId) {
        return enqueue(type, List.of(playerId));
    }

    /**
     * Dequeues a group of players from their current queues.
     *
     * @param playerIds a list of UUIDs of the players to remove from their queues
     * @return a future completing with the result of the dequeue operation
     */
    CompletableFuture<DequeueResponse> dequeue(List<UUID> playerIds);

    /**
     * Dequeues a single player from their current queue.
     *
     * @param playerId the UUID of the player to remove from their queue
     * @return a future completing with the result of the dequeue operation
     */
    default CompletableFuture<DequeueResponse> dequeue(UUID playerId) {
        return dequeue(List.of(playerId));
    }
}

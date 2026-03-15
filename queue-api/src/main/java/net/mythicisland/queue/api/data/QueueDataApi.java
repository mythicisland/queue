package net.mythicisland.queue.api.data;

import build.buf.gen.mythicisland.queue.v1.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * API for queue data queries.
 *
 * <p>Provides read access to queues, queue types, and player positions
 * for use in GUIs, tab completion, scoreboards, etc.</p>
 */
public interface QueueDataApi {

    /**
     * Gets a single queue by its ID.
     *
     * @param queueId the unique ID of the queue
     * @return a future completing with the queue, or failing with {@code NOT_FOUND}
     */
    CompletableFuture<GetQueueResponse> getQueue(UUID queueId);

    /**
     * Gets all active queues.
     *
     * @return a future completing with all queues
     */
    CompletableFuture<GetAllQueuesResponse> getAllQueues();

    /**
     * Gets all active queues of a specific type.
     *
     * @param type the queue type name (e.g. "dev")
     * @return a future completing with the matching queues
     */
    CompletableFuture<GetQueuesByTypeResponse> getQueuesByType(String type);

    /**
     * Gets the queue a player is currently in.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the player's queue, or failing with {@code NOT_FOUND}
     */
    CompletableFuture<GetQueueByPlayerResponse> getQueueByPlayer(UUID playerId);

    /**
     * Gets a player's position in their current queue.
     *
     * <p>The position is 1-based (first player = position 1).</p>
     *
     * @param playerId the UUID of the player
     * @return a future completing with the queue and position, or failing with {@code NOT_FOUND}
     */
    CompletableFuture<GetPlayerPositionResponse> getPlayerPosition(UUID playerId);

    /**
     * Gets a single queue type configuration by its name.
     *
     * @param name the queue type name
     * @return a future completing with the queue type, or failing with {@code NOT_FOUND}
     */
    CompletableFuture<GetQueueTypeResponse> getQueueType(String name);

    /**
     * Gets all available queue types.
     *
     * @return a future completing with all queue type configurations
     */
    CompletableFuture<GetAllQueueTypesResponse> getAllQueueTypes();
}

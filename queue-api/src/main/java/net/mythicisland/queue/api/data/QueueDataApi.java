package net.mythicisland.queue.api.data;

import build.buf.gen.mythicisland.queue.v1.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * API for querying queue data.
 */
public interface QueueDataApi {

    /**
     * Retrieves information about a specific queue.
     *
     * @param queueId the unique identifier of the queue
     * @return a future completing with the queue details
     */
    CompletableFuture<GetQueueResponse> getQueue(UUID queueId);

    /**
     * Retrieves a list of all currently active queues across all types.
     *
     * @return a future completing with a list of all active queues
     */
    CompletableFuture<GetAllQueuesResponse> getAllQueues();

    /**
     * Retrieves all active queues of a specific type (e.g., "skyblock", "bedwars").
     *
     * @param type the name of the queue type
     * @return a future completing with a list of matching queues
     */
    CompletableFuture<GetQueuesByTypeResponse> getQueuesByType(String type);

    /**
     * Finds the queue that a specific player is currently a member of.
     *
     * @param playerId the unique identifier of the player
     * @return a future completing with the queue containing the player, or an empty response if not in any queue
     */
    CompletableFuture<GetQueueByPlayerResponse> getQueueByPlayer(UUID playerId);

    /**
     * Retrieves a player's current position and progress within their queue.
     *
     * @param playerId the unique identifier of the player
     * @return a future completing with the player's position information
     */
    CompletableFuture<GetPlayerPositionResponse> getPlayerPosition(UUID playerId);

    /**
     * Retrieves the configuration and metadata for a specific queue type.
     *
     * @param name the name of the queue type
     * @return a future completing with the queue type configuration
     */
    CompletableFuture<GetQueueTypeResponse> getQueueType(String name);

    /**
     * Retrieves a list of all registered queue types and their configurations.
     *
     * @return a future completing with all available queue types
     */
    CompletableFuture<GetAllQueueTypesResponse> getAllQueueTypes();
}

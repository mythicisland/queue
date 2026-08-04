package net.mythicisland.queue.api.data;

import build.buf.gen.mythicisland.queue.v2.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * API for querying tickets, matches and the queue configuration.
 */
public interface QueueDataApi {

    /**
     * Retrieves a specific ticket.
     *
     * @param ticketId the unique identifier of the ticket
     * @return a future completing with the ticket
     */
    CompletableFuture<GetTicketResponse> getTicket(UUID ticketId);

    /**
     * Finds the ticket a player belongs to.
     *
     * @param playerId the unique identifier of the player
     * @return a future completing with the ticket containing the player
     */
    CompletableFuture<GetTicketByPlayerResponse> getTicketByPlayer(UUID playerId);

    /**
     * Retrieves every ticket currently in matchmaking.
     *
     * @return a future completing with all tickets
     */
    CompletableFuture<ListTicketsResponse> listTickets();

    /**
     * Retrieves every ticket searching in a specific queue type.
     *
     * @param queueType the name of the queue type
     * @return a future completing with the matching tickets
     */
    CompletableFuture<ListTicketsResponse> listTickets(String queueType);

    /**
     * Retrieves a specific match.
     *
     * @param matchId the unique identifier of the match
     * @return a future completing with the match
     */
    CompletableFuture<GetMatchResponse> getMatch(UUID matchId);

    /**
     * Retrieves every match that has not finished yet.
     *
     * @return a future completing with all active matches
     */
    CompletableFuture<ListMatchesResponse> listMatches();

    /**
     * Retrieves every active match of a specific queue type.
     *
     * @param queueType the name of the queue type
     * @return a future completing with the matching matches
     */
    CompletableFuture<ListMatchesResponse> listMatches(String queueType);

    /**
     * Retrieves the configuration of a specific queue type.
     *
     * @param name the name of the queue type
     * @return a future completing with the queue type configuration
     */
    CompletableFuture<GetQueueTypeResponse> getQueueType(String name);

    /**
     * Retrieves every registered queue type and its configuration.
     *
     * @return a future completing with all available queue types
     */
    CompletableFuture<ListQueueTypesResponse> listQueueTypes();

    /**
     * Retrieves the live numbers of a queue type, for example to show how many
     * players are currently searching.
     *
     * @param queueType the name of the queue type
     * @return a future completing with the queue statistics
     */
    CompletableFuture<GetQueueStatsResponse> getQueueStats(String queueType);

    /**
     * Retrieves the live numbers of every queue type.
     *
     * @return a future completing with the statistics of all queue types
     */
    CompletableFuture<ListQueueStatsResponse> listQueueStats();

}

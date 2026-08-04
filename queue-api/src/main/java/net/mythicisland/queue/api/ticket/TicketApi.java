package net.mythicisland.queue.api.ticket;

import build.buf.gen.mythicisland.queue.v2.CreateTicketResponse;
import build.buf.gen.mythicisland.queue.v2.DeleteTicketResponse;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * API for putting players into matchmaking and taking them back out.
 */
public interface TicketApi {

    /**
     * Creates a ticket for a party, searching in one or more queue types.
     *
     * <p>Passing several queue types means the party joins whichever match fills up first.</p>
     *
     * @param playerIds the UUIDs of the players to enqueue together
     * @param queueTypes the names of the queue types to search in
     * @return a future completing with the created ticket
     */
    CompletableFuture<CreateTicketResponse> createTicket(List<UUID> playerIds, List<String> queueTypes);

    /**
     * Creates a ticket for a single player, searching in one or more queue types.
     *
     * @param playerId the UUID of the player to enqueue
     * @param queueTypes the names of the queue types to search in
     * @return a future completing with the created ticket
     */
    default CompletableFuture<CreateTicketResponse> createTicket(UUID playerId, List<String> queueTypes) {
        return createTicket(List.of(playerId), queueTypes);
    }

    /**
     * Creates a ticket for a single player, searching in one queue type.
     *
     * @param playerId the UUID of the player to enqueue
     * @param queueType the name of the queue type to search in
     * @return a future completing with the created ticket
     */
    default CompletableFuture<CreateTicketResponse> createTicket(UUID playerId, String queueType) {
        return createTicket(List.of(playerId), List.of(queueType));
    }

    /**
     * Removes a ticket from matchmaking.
     *
     * @param ticketId the unique ID of the ticket
     * @return a future completing when the ticket was removed
     */
    CompletableFuture<DeleteTicketResponse> deleteTicket(UUID ticketId);

    /**
     * Removes the ticket a player belongs to, including the rest of their party.
     *
     * @param playerId the UUID of the player
     * @return a future completing when the ticket was removed
     */
    CompletableFuture<DeleteTicketResponse> deleteTicketByPlayer(UUID playerId);

}

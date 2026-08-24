package net.mythicisland.queue.api.ticket;

import net.mythicisland.queue.api.match.Assignment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A single player or a group of players.
 *
 * @param id the unique ID of the ticket
 * @param playerIds the players behind the ticket, one entry means solo
 * @param queueTypes the queue types the ticket is searching in
 * @param state the current state of the ticket
 * @param createdAt when the ticket entered matchmaking
 * @param matchId the match the ticket was put into, null while searching
 * @param assignment the server to connect to, null until one was allocated
 * @param countdownEndsAt when the players get transferred, null until a server was allocated
 */
public record Ticket(
        UUID id,
        List<UUID> playerIds,
        List<String> queueTypes,
        TicketState state,
        Instant createdAt,
        UUID matchId,
        Assignment assignment,
        Instant countdownEndsAt
) {
}

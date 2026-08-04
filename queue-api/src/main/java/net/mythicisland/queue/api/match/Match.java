package net.mythicisland.queue.api.match;

import net.mythicisland.queue.api.ticket.Ticket;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A set of tickets that will play together on one game server.
 *
 * @param id the unique ID of the match
 * @param queueType the queue type the match was created for
 * @param tickets the tickets forming the match
 * @param state the current state of the match
 * @param createdAt when the match was formed
 * @param assignment the allocated server, null while allocating
 * @param countdownEndsAt when the players get transferred, null until a server was allocated
 */
public record Match(
        UUID id,
        String queueType,
        List<Ticket> tickets,
        MatchState state,
        Instant createdAt,
        Assignment assignment,
        Instant countdownEndsAt
) {
}

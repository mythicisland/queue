package net.mythicisland.queue.api.event.match;

import net.mythicisland.queue.api.match.Match;

import java.util.List;
import java.util.UUID;

/**
 * Event fired after the players of a match were sent to their game server.
 *
 * <p>Players that were offline or failed to connect are missing from
 * {@code transferredPlayerIds}.</p>
 *
 * @param match the transferred match
 * @param transferredPlayerIds the UUIDs of the players that actually made it onto the server
 */
public record MatchTransferredEvent(
        Match match,
        List<UUID> transferredPlayerIds
) {
}

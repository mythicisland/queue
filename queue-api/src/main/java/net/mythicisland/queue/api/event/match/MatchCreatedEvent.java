package net.mythicisland.queue.api.event.match;

import net.mythicisland.queue.api.match.Match;

/**
 * Event fired when enough tickets were found to form a match.
 *
 * @param match the created match
 */
public record MatchCreatedEvent(
        Match match
) {
}

package net.mythicisland.queue.api.event.match;

import net.mythicisland.queue.api.match.Match;
import net.mythicisland.queue.api.match.MatchState;

/**
 * Event fired when a match transitions from one state to another.
 *
 * @param match the match, already carrying its new state
 * @param previousState the state the match was in before
 */
public record MatchStateChangedEvent(
        Match match,
        MatchState previousState
) {
}

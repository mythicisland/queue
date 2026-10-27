package net.mythicisland.queue.api.event.match

import net.mythicisland.queue.api.match.Match
import net.mythicisland.queue.api.match.MatchState

/**
 * Fired when a match changed from [previousState] to the state it has now.
 */
data class MatchStateChangedEvent(
    val match: Match,
    val previousState: MatchState,
)

package net.mythicisland.queue.api.event.match

import net.mythicisland.queue.api.match.Match
import net.mythicisland.queue.api.match.MatchReleaseReason

/**
 * Fired when a match was released.
 */
data class MatchReleasedEvent(
    val match: Match,
    val reason: MatchReleaseReason,
)

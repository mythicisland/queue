package net.mythicisland.queue.api.event.match

import net.mythicisland.queue.api.match.Match

/**
 * Fired when a match was created.
 */
data class MatchCreatedEvent(
    val match: Match,
)

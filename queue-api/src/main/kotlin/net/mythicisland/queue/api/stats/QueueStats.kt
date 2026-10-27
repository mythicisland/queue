package net.mythicisland.queue.api.stats

import net.mythicisland.queue.api.match.MatchState

/**
 * A snapshot of one queue type.
 *
 * [activeMatches] counts the matches whose players aren't on their server yet, [MatchState.COMPLETED] ones don't count.
 */
data class QueueStats(
    val queueType: String,
    val searchingTickets: Int,
    val searchingPlayers: Int,
    val activeMatches: Int,
)

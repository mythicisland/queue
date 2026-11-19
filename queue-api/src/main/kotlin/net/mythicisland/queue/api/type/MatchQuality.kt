package net.mythicisland.queue.api.type

/**
 * Limits for a whole match. They grow with the wait time of the oldest ticket, so players wait for a better match
 * first and take a worse one later.
 *
 * [spread] limits how far the values of a property may be apart in one match.
 * [teamBalance] limits the difference between the team averages of [TeamConfig.balanceBy], `null` means no limit.
 */
data class MatchQuality(
    val spread: List<SpreadLimit> = emptyList(),
    val teamBalance: Window? = null,
)

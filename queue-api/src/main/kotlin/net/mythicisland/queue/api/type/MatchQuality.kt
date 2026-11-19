package net.mythicisland.queue.api.type

import java.time.Duration

/**
 * Limits for a whole match. They grow with the wait time of the oldest ticket, so players wait for a better match
 * first and take a worse one later.
 *
 * [spread] limits how far the values of a property may be apart in one match.
 * [teamBalance] limits the difference between the team averages of [TeamConfig.balanceBy], `null` means no limit.
 */
data class MatchQuality(
    val spread: List<SpreadLimit>,
    val teamBalance: Window?,
)

/**
 * The highest minus the lowest numeric value of [key] in a match may be at most [window].
 *
 * Tickets in a queue type with this limit must have [key] set to a number,
 * otherwise creating or updating them fails with [net.mythicisland.queue.api.QueueException.Code.INVALID_ARGUMENT].
 */
data class SpreadLimit(
    val key: String,
    val window: Window,
)

/**
 * A limit that starts at [start] and grows by [widen] every [every], up to [max] if it is set.
 */
data class Window(
    val start: Double,
    val widen: Double,
    val every: Duration?,
    val max: Double?,
)

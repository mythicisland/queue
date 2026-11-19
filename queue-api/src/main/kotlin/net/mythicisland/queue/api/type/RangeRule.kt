package net.mythicisland.queue.api.type

import java.time.Duration

/**
 * The numeric values of [key] may differ by at most [start] from the oldest ticket.
 * The allowed difference grows by [widen] every [every] the oldest ticket waits, up to [max] if it is set.
 *
 * Tickets in a queue type with this rule must have [key] set to a number,
 * otherwise creating or updating them fails with [net.mythicisland.queue.api.QueueException.Code.INVALID_ARGUMENT].
 */
data class RangeRule(
    override val key: String,
    val start: Double,
    val widen: Double = 0.0,
    val every: Duration? = null,
    val max: Double? = null,
) : MatchRule

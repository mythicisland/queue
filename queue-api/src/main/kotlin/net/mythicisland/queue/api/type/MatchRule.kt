package net.mythicisland.queue.api.type

import java.time.Duration

/**
 * A rule that decides which tickets may play in the same match.
 *
 * Rules compare a ticket property with the one of the ticket that waits the longest,
 * the queue doesn't know what the values mean.
 */
sealed interface MatchRule {

    /** The ticket property the rule compares. */
    val key: String

}

/**
 * Only tickets with the same value for [key] play together, tickets without it count as one more value.
 * The rule no longer applies once the oldest ticket waited [relaxAfter], if it is set.
 */
data class EqualRule(
    override val key: String,
    val relaxAfter: Duration?,
) : MatchRule

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
    val widen: Double,
    val every: Duration?,
    val max: Double?,
) : MatchRule

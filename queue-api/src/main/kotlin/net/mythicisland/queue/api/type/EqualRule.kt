package net.mythicisland.queue.api.type

import java.time.Duration

/**
 * Only tickets with the same value for [key] play together, tickets without it count as one more value.
 * The rule no longer applies once the oldest ticket waited [relaxAfter], if it is set.
 */
data class EqualRule(
    override val key: String,
    val relaxAfter: Duration? = null,
) : MatchRule

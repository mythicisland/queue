package net.mythicisland.queue.api.type

/**
 * The value of [key] must be a number from [min] to [max], both inclusive. A bound that is `null` doesn't apply.
 */
data class NumberFilter(
    override val key: String,
    val min: Double? = null,
    val max: Double? = null,
) : MatchFilter

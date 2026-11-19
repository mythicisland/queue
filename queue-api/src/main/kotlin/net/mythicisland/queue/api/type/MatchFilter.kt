package net.mythicisland.queue.api.type

/**
 * A fixed condition every ticket must pass to search in a queue type. Unlike a [MatchRule] it never relaxes.
 *
 * Creating or updating a ticket that doesn't pass the filters of one of its queue types fails with
 * [net.mythicisland.queue.api.QueueException.Code.INVALID_ARGUMENT].
 */
sealed interface MatchFilter {

    /** The ticket property the filter checks. */
    val key: String

}

/**
 * The value of [key] must be one of [values].
 */
data class ValuesFilter(
    override val key: String,
    val values: List<String>,
) : MatchFilter

/**
 * The value of [key] must be a number from [min] to [max], both inclusive. A bound that is `null` doesn't apply.
 */
data class NumberFilter(
    override val key: String,
    val min: Double? = null,
    val max: Double? = null,
) : MatchFilter

/**
 * [key] must be set if [present] is `true`, or must not be set if it is `false`.
 */
data class PresenceFilter(
    override val key: String,
    val present: Boolean,
) : MatchFilter

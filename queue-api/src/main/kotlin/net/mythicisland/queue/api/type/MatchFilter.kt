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

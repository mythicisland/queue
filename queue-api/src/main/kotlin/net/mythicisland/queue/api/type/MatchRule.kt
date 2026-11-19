package net.mythicisland.queue.api.type

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

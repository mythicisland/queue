package net.mythicisland.queue.api.type

/**
 * [count] teams with [size] players each.
 *
 * If [balanceBy] is set, the teams are balanced by the sum of that ticket property, e.g. a rating.
 * The value counts for every player of the ticket, so a party sets the average of its players.
 */
data class TeamConfig(
    val count: Int,
    val size: Int,
    val balanceBy: String? = null,
)

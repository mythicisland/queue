package net.mythicisland.queue.api.type

import java.time.Duration

/**
 * How matches of a queue type are put together.
 *
 * A match starts once [minPlayers] are found and either [maxPlayers] is reached or [fillTime] ran out.
 * [maxPlayers] is `count * size` if there are [teams]. Only tickets that pass all [rules] play in the same match.
 *
 * Only tickets that pass all [filters] search in this queue type, and every match stays within the limits of [quality].
 *
 * A ticket stops searching in this queue type once it waited [maxWait] or had [maxFailures] failed matches in it,
 * `null` means no limit. It is deleted once it doesn't search in any of its queue types anymore.
 *
 * When creating or updating a queue type, [maxPlayers] is ignored if there are [teams], and a [PartyConfig.maxSize]
 * of `0` means the team size, or [maxPlayers] without teams.
 */
data class MatchConfig(
    val minPlayers: Int,
    val maxPlayers: Int,
    val fillTime: Duration,
    val countdown: Duration,
    val teams: TeamConfig? = null,
    val parties: PartyConfig = PartyConfig(0),
    val rules: List<MatchRule> = emptyList(),
    val maxWait: Duration? = null,
    val maxFailures: Int? = null,
    val filters: List<MatchFilter> = emptyList(),
    val quality: MatchQuality = MatchQuality(),
)

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

/**
 * Tickets have at most [maxSize] players.
 */
data class PartyConfig(
    val maxSize: Int,
)

package net.mythicisland.queue.api.type

import java.time.Duration

/**
 * How matches of a queue type are put together.
 *
 * A match starts once [minPlayers] are found and either [maxPlayers] is reached or [fillTime] ran out.
 * [maxPlayers] is `count * size` if there are [teams].
 */
data class MatchConfig(
    val minPlayers: Int,
    val maxPlayers: Int,
    val fillTime: Duration,
    val countdown: Duration,
    val teams: TeamConfig?,
    val parties: PartyConfig,
)

/**
 * [count] teams with [size] players each.
 */
data class TeamConfig(
    val count: Int,
    val size: Int,
)

/**
 * Tickets have at most [maxSize] players.
 */
data class PartyConfig(
    val maxSize: Int,
)

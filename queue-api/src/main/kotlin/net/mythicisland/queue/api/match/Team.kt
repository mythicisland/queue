package net.mythicisland.queue.api.match

import java.util.UUID

/**
 * The players of one team in a match.
 */
data class Team(
    val playerIds: List<UUID>,
)

package net.mythicisland.queue.api.event.match

import net.mythicisland.queue.api.match.Match
import java.util.UUID

/**
 * Fired when the players of a match were transferred.
 *
 * [missingPlayerIds] contains the players that didn't make it onto the server.
 */
data class MatchTransferredEvent(
    val match: Match,
    val transferredPlayerIds: List<UUID>,
    val missingPlayerIds: List<UUID>,
)

package net.mythicisland.queue.api.ticket

import net.mythicisland.queue.api.match.Assignment
import java.time.Instant
import java.util.UUID

/**
 * One or more players queueing together for one or more queue types.
 *
 * [matchId] is `null` while searching, [assignment] and [countdownEndsAt] are `null` until the match has a server.
 */
data class Ticket(
    val id: UUID,
    val playerIds: List<UUID>,
    val queueTypes: List<String>,
    val state: TicketState,
    val createdAt: Instant,
    val matchId: UUID?,
    val assignment: Assignment?,
    val countdownEndsAt: Instant?,
    val properties: Map<String, String>,
)

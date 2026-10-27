package net.mythicisland.queue.api.match

import net.mythicisland.queue.api.ticket.Ticket
import java.time.Instant
import java.util.UUID

/**
 * A group of tickets split into teams, plus the server it runs on.
 *
 * [assignment] and [countdownEndsAt] are `null` until the match has a server.
 */
data class Match(
    val id: UUID,
    val queueType: String,
    val tickets: List<Ticket>,
    val teams: List<Team>,
    val state: MatchState,
    val createdAt: Instant,
    val assignment: Assignment?,
    val countdownEndsAt: Instant?,
)

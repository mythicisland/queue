package net.mythicisland.queue.runtime

import build.buf.gen.mythicisland.queue.v2.MatchState
import build.buf.gen.mythicisland.queue.v2.TicketState
import net.mythicisland.queue.shared.match.Match
import net.mythicisland.queue.shared.match.Ticket
import java.time.Instant
import java.util.UUID

/**
 * A fixed point in time, so nothing in the tests depends on the clock.
 */
val NOW: Instant = Instant.parse("2026-01-01T00:00:00Z")

/**
 * Creates the given amount of player ids.
 */
fun players(amount: Int): List<UUID> = List(amount) { UUID.randomUUID() }

fun ticket(
    playerIds: List<UUID> = players(1),
    queueTypes: List<String> = listOf("battle"),
    state: TicketState = TicketState.TICKET_STATE_SEARCHING,
    createdAt: Instant = NOW,
): Ticket = Ticket(
    id = UUID.randomUUID(),
    playerIds = playerIds,
    queueTypes = queueTypes,
    state = state,
    createdAt = createdAt,
)

fun match(
    ticketIds: List<UUID> = listOf(UUID.randomUUID()),
    queueType: String = "battle",
    state: MatchState = MatchState.MATCH_STATE_ALLOCATING,
): Match = Match(
    id = UUID.randomUUID(),
    queueType = queueType,
    ticketIds = ticketIds,
    state = state,
    createdAt = NOW,
)

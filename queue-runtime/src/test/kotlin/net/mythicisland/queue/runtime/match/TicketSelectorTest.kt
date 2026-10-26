package net.mythicisland.queue.runtime.match

import net.mythicisland.queue.shared.ticket.Ticket
import net.mythicisland.queue.shared.ticket.TicketState
import net.mythicisland.queue.shared.type.MatchConfig
import net.mythicisland.queue.shared.type.QueueType
import net.mythicisland.queue.shared.type.ServerConfig
import net.mythicisland.queue.shared.type.TeamConfig
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TicketSelectorTest {

    private val start = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `selects nothing below min players`() {
        assertNull(TicketSelector.select(ffa(min = 3, max = 8), listOf(ticket(0), ticket(1)), start.plusSeconds(60)))
    }

    @Test
    fun `selects a full match immediately`() {
        val tickets = List(4) { ticket(it.toLong()) }

        assertEquals(tickets, TicketSelector.select(ffa(min = 2, max = 4, fillTime = 30), tickets, start.plusSeconds(3)))
    }

    @Test
    fun `waits for the fill time once min players is reached`() {
        val tickets = listOf(ticket(0), ticket(10))
        val type = ffa(min = 2, max = 4, fillTime = 30)

        assertNull(TicketSelector.select(type, tickets, start.plusSeconds(20)))
        assertEquals(tickets, TicketSelector.select(type, tickets, start.plusSeconds(40)))
    }

    @Test
    fun `prefers the oldest tickets`() {
        val young = ticket(5)
        val old = ticket(0)
        val older = ticket(-5)

        assertEquals(listOf(older, old), TicketSelector.select(ffa(min = 2, max = 2), listOf(young, old, older), start.plusSeconds(10)))
    }

    @Test
    fun `skips tickets that do not fit`() {
        val single = ticket(0)
        val party = ticket(1, players = 3)
        val other = ticket(2)

        assertEquals(listOf(single, other), TicketSelector.select(ffa(min = 2, max = 3), listOf(single, party, other), start.plusSeconds(10)))
    }

    @Test
    fun `respects team limits for parties`() {
        val type = QueueType(
            server = ServerConfig(group = "teams"),
            match = MatchConfig(minPlayers = 2, teams = TeamConfig(count = 2, size = 3)),
        )
        val tickets = listOf(ticket(0, players = 2), ticket(1, players = 2), ticket(2, players = 2), ticket(3))
        val selected = assertNotNull(TicketSelector.select(type, tickets, start.plusSeconds(10)))

        assertEquals(listOf(tickets[0], tickets[1], tickets[3]), selected)
    }

    private fun ffa(min: Int, max: Int, fillTime: Long = 0): QueueType = QueueType(
        server = ServerConfig(group = "ffa"),
        match = MatchConfig(minPlayers = min, maxPlayers = max, fillTime = Duration.ofSeconds(fillTime)),
    )

    private fun ticket(createdAfterSeconds: Long, players: Int = 1): Ticket = Ticket(
        id = UUID.randomUUID().toString(),
        playerIds = List(players) { UUID.randomUUID() },
        queueTypes = listOf("ffa"),
        state = TicketState.SEARCHING,
        createdAt = start.plusSeconds(createdAfterSeconds),
    )

}

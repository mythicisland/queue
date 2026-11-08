package net.mythicisland.queue.runtime.match

import net.mythicisland.queue.shared.ticket.Ticket
import net.mythicisland.queue.shared.ticket.TicketState
import net.mythicisland.queue.shared.type.MatchConfig
import net.mythicisland.queue.shared.type.MatchRule
import net.mythicisland.queue.shared.type.MatchRuleType
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

        assertEquals(tickets, TicketSelector.select(ffa(min = 2, max = 4, fillTime = 30), tickets, start.plusSeconds(3))?.tickets)
    }

    @Test
    fun `waits for the fill time once min players is reached`() {
        val tickets = listOf(ticket(0), ticket(10))
        val type = ffa(min = 2, max = 4, fillTime = 30)

        assertNull(TicketSelector.select(type, tickets, start.plusSeconds(20)))
        assertEquals(tickets, TicketSelector.select(type, tickets, start.plusSeconds(40))?.tickets)
    }

    @Test
    fun `prefers the oldest tickets`() {
        val young = ticket(5)
        val old = ticket(0)
        val older = ticket(-5)

        assertEquals(listOf(older, old), TicketSelector.select(ffa(min = 2, max = 2), listOf(young, old, older), start.plusSeconds(10))?.tickets)
    }

    @Test
    fun `skips tickets that do not fit`() {
        val single = ticket(0)
        val party = ticket(1, players = 3)
        val other = ticket(2)

        assertEquals(listOf(single, other), TicketSelector.select(ffa(min = 2, max = 3), listOf(single, party, other), start.plusSeconds(10))?.tickets)
    }

    @Test
    fun `respects team limits for parties`() {
        val type = QueueType(
            server = ServerConfig(group = "teams"),
            match = MatchConfig(minPlayers = 2, teams = TeamConfig(count = 2, size = 3)),
        )
        val tickets = listOf(ticket(0, players = 2), ticket(1, players = 2), ticket(2, players = 2), ticket(3))
        val selected = assertNotNull(TicketSelector.select(type, tickets, start.plusSeconds(10))).tickets

        assertEquals(listOf(tickets[0], tickets[1], tickets[3]), selected)
    }

    @Test
    fun `equal rule only matches tickets with the same value`() {
        val eu = ticket(0, properties = mapOf("region" to "eu"))
        val us = ticket(1, properties = mapOf("region" to "us"))
        val otherEu = ticket(2, properties = mapOf("region" to "eu"))
        val type = ffa(min = 2, max = 2, rules = listOf(equal("region")))

        assertEquals(listOf(eu, otherEu), TicketSelector.select(type, listOf(eu, us, otherEu), start.plusSeconds(10))?.tickets)
    }

    @Test
    fun `equal rule no longer applies once the oldest ticket waited relax after`() {
        val tickets = listOf(ticket(0, properties = mapOf("region" to "eu")), ticket(1, properties = mapOf("region" to "us")))
        val type = ffa(min = 2, max = 2, rules = listOf(equal("region", relaxAfter = 30)))

        assertNull(TicketSelector.select(type, tickets, start.plusSeconds(20)))
        assertEquals(tickets, TicketSelector.select(type, tickets, start.plusSeconds(30))?.tickets)
    }

    @Test
    fun `range rule rejects tickets outside the window`() {
        val tickets = listOf(rated(0, 1000), rated(1, 1200))
        val type = ffa(min = 2, max = 2, rules = listOf(range("rating", start = 100.0)))

        assertNull(TicketSelector.select(type, tickets, start.plusSeconds(600)))
    }

    @Test
    fun `range window widens with the wait time of the oldest ticket`() {
        val tickets = listOf(rated(0, 1000), rated(0, 1150))
        val type = ffa(min = 2, max = 2, rules = listOf(range("rating", start = 100.0, widen = 50.0, every = 10)))

        assertNull(TicketSelector.select(type, tickets, start.plusSeconds(9)))
        assertEquals(tickets, TicketSelector.select(type, tickets, start.plusSeconds(10))?.tickets)
    }

    @Test
    fun `range window never grows beyond max`() {
        val tickets = listOf(rated(0, 1000), rated(0, 1400))
        val type = ffa(min = 2, max = 2, rules = listOf(range("rating", start = 100.0, widen = 100.0, every = 10, max = 300.0)))

        assertNull(TicketSelector.select(type, tickets, start.plusSeconds(3600)))
    }

    @Test
    fun `an unmatched oldest ticket does not block younger tickets`() {
        val outlier = rated(0, 3000)
        val first = rated(1, 1000)
        val second = rated(2, 1050)
        val type = ffa(min = 2, max = 2, rules = listOf(range("rating", start = 100.0)))

        assertEquals(listOf(first, second), TicketSelector.select(type, listOf(outlier, first, second), start.plusSeconds(10))?.tickets)
    }

    @Test
    fun `range compares every ticket with the oldest ticket of the match`() {
        val anchor = rated(0, 1000)
        val low = rated(1, 900)
        val high = rated(2, 1100)
        val type = ffa(min = 3, max = 3, rules = listOf(range("rating", start = 100.0)))

        assertEquals(listOf(anchor, low, high), TicketSelector.select(type, listOf(anchor, low, high), start.plusSeconds(10))?.tickets)
    }

    private fun ffa(min: Int, max: Int, fillTime: Long = 0, rules: List<MatchRule> = emptyList()): QueueType = QueueType(
        server = ServerConfig(group = "ffa"),
        match = MatchConfig(minPlayers = min, maxPlayers = max, fillTime = Duration.ofSeconds(fillTime), rules = rules),
    )

    private fun equal(key: String, relaxAfter: Long? = null): MatchRule = MatchRule(
        type = MatchRuleType.EQUAL,
        key = key,
        relaxAfter = if (relaxAfter == null) null else Duration.ofSeconds(relaxAfter),
    )

    private fun range(key: String, start: Double, widen: Double? = null, every: Long? = null, max: Double? = null): MatchRule = MatchRule(
        type = MatchRuleType.RANGE,
        key = key,
        start = start,
        widen = widen,
        every = if (every == null) null else Duration.ofSeconds(every),
        max = max,
    )

    private fun rated(createdAfterSeconds: Long, rating: Int): Ticket =
        ticket(createdAfterSeconds, properties = mapOf("rating" to rating.toString()))

    private fun ticket(createdAfterSeconds: Long, players: Int = 1, properties: Map<String, String> = emptyMap()): Ticket = Ticket(
        id = UUID.randomUUID().toString(),
        playerIds = List(players) { UUID.randomUUID() },
        queueTypes = listOf("ffa"),
        state = TicketState.SEARCHING,
        createdAt = start.plusSeconds(createdAfterSeconds),
        properties = properties,
    )

}

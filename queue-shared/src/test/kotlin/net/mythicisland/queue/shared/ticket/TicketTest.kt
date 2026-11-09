package net.mythicisland.queue.shared.ticket

import net.mythicisland.queue.shared.type.MatchConfig
import net.mythicisland.queue.shared.type.QueueType
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TicketTest {

    private val start = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `a ticket without limits never expires`() {
        assertFalse(ticket().isExpired(listOf(type("ffa")), start.plusSeconds(86400)))
    }

    @Test
    fun `a ticket keeps searching in its other queue types after one expired`() {
        val types = listOf(type("ffa", maxWait = 60), type("teams", maxWait = 300))

        assertFalse(ticket().isExpired(types, start.plusSeconds(120)))
    }

    @Test
    fun `a ticket expires once none of its queue types is left`() {
        val types = listOf(type("ffa", maxWait = 60), type("teams", maxFailures = 1))

        assertTrue(ticket().failed("teams").isExpired(types, start.plusSeconds(60)))
    }

    @Test
    fun `a ticket expires when none of its queue types exists anymore`() {
        assertTrue(ticket().isExpired(emptyList(), start))
    }

    @Test
    fun `failures are counted per queue type`() {
        val failed = ticket().failed("ffa").failed("ffa").failed("teams")

        assertEquals(mapOf("ffa" to 2, "teams" to 1), failed.failures)
    }

    private fun type(name: String, maxWait: Long? = null, maxFailures: Int? = null): QueueType = QueueType(
        name = name,
        match = MatchConfig(
            minPlayers = 2,
            maxPlayers = 8,
            maxWait = if (maxWait == null) null else Duration.ofSeconds(maxWait),
            maxFailures = maxFailures,
        ),
    )

    private fun ticket(): Ticket = Ticket(
        id = UUID.randomUUID().toString(),
        playerIds = listOf(UUID.randomUUID()),
        queueTypes = listOf("ffa", "teams"),
        state = TicketState.SEARCHING,
        createdAt = start,
    )

}

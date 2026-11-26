package net.mythicisland.queue.runtime.listener

import net.mythicisland.queue.shared.ticket.Ticket
import net.mythicisland.queue.shared.ticket.TicketState
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerListenerTest {

    private val now = Instant.parse("2026-11-20T12:00:00Z")
    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()

    @Test
    fun `a ticket is kept the first time its player is missing`() {
        val ticket = ticket("a", alice)

        val result = PlayerListener.findDisconnected(listOf(ticket), setOf(bob), emptySet(), now)

        assertTrue(result.disconnected.isEmpty())
        assertEquals(setOf("a"), result.missing)
    }

    @Test
    fun `a ticket is dropped when its player is missing twice in a row`() {
        val ticket = ticket("a", alice)

        val result = PlayerListener.findDisconnected(listOf(ticket), setOf(bob), setOf("a"), now)

        assertEquals(listOf(ticket), result.disconnected)
        assertTrue(result.missing.isEmpty())
    }

    @Test
    fun `a player who comes back in time keeps the ticket`() {
        val ticket = ticket("a", alice)

        val result = PlayerListener.findDisconnected(listOf(ticket), setOf(alice), setOf("a"), now)

        assertTrue(result.disconnected.isEmpty())
        assertTrue(result.missing.isEmpty())
    }

    @Test
    fun `nobody online is treated as a broken player list and drops nothing`() {
        val ticket = ticket("a", alice)

        val result = PlayerListener.findDisconnected(listOf(ticket), emptySet(), setOf("a"), now)

        assertTrue(result.disconnected.isEmpty())
        assertEquals(setOf("a"), result.missing)
    }

    @Test
    fun `a party ticket is dropped when one of its players is gone`() {
        val ticket = ticket("a", alice, bob)

        val result = PlayerListener.findDisconnected(listOf(ticket), setOf(alice), setOf("a"), now)

        assertEquals(listOf(ticket), result.disconnected)
    }

    @Test
    fun `a ticket created during the sync is not judged by the player list`() {
        val ticket = ticket("a", alice, createdAt = now.plusSeconds(1))

        val result = PlayerListener.findDisconnected(listOf(ticket), setOf(bob), setOf("a"), now)

        assertTrue(result.disconnected.isEmpty())
        assertTrue(result.missing.isEmpty())
    }

    private fun ticket(id: String, vararg players: UUID, createdAt: Instant = now.minusSeconds(60)) =
        Ticket(id, players.toList(), listOf("duels"), TicketState.SEARCHING, createdAt)

}

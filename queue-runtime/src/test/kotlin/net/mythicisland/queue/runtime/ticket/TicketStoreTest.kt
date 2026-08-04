package net.mythicisland.queue.runtime.ticket

import build.buf.gen.mythicisland.queue.v2.TicketState
import kotlinx.coroutines.runBlocking
import net.mythicisland.queue.runtime.players
import net.mythicisland.queue.runtime.ticket
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TicketStoreTest {

    @Test
    fun `finds a ticket by every one of its players`() {
        runBlocking {
            val store = TicketStore()
            val ticket = ticket(playerIds = players(3))

            assertTrue(store.add(ticket))
            assertEquals(ticket, store.get(ticket.id))
            ticket.playerIds.forEach { assertEquals(ticket, store.getByPlayer(it)) }
        }
    }

    @Test
    fun `rejects a ticket when one of its players is already queued`() {
        runBlocking {
            val store = TicketStore()
            val queued = players(1)
            store.add(ticket(playerIds = queued))

            val second = ticket(playerIds = queued + players(1))

            assertFalse(store.add(second))
            assertEquals(1, store.getAll().size)
            assertNull(store.get(second.id))
        }
    }

    @Test
    fun `removing a ticket frees its players`() {
        runBlocking {
            val store = TicketStore()
            val ticket = ticket(playerIds = players(2))
            store.add(ticket)

            assertEquals(ticket, store.remove(ticket.id))
            assertNull(store.getByPlayer(ticket.playerIds.first()))

            // The players can queue again right away.
            assertTrue(store.add(ticket(playerIds = ticket.playerIds)))
        }
    }

    @Test
    fun `ignores an update of a ticket that was removed`() {
        runBlocking {
            val store = TicketStore()
            val ticket = ticket()

            assertNull(store.update(ticket))
        }
    }

    @Test
    fun `matched moves every ticket into the match`() {
        runBlocking {
            val store = TicketStore()
            val first = ticket()
            val second = ticket()
            store.add(first)
            store.add(second)

            val matchId = UUID.randomUUID()
            val matched = store.matched(listOf(first.id, second.id), matchId)

            assertNotNull(matched)
            assertEquals(2, matched.size)
            assertTrue(matched.all { it.state == TicketState.TICKET_STATE_MATCHED && it.matchId == matchId })
            assertEquals(TicketState.TICKET_STATE_MATCHED, store.get(first.id)?.state)
        }
    }

    @Test
    fun `matched fails when a ticket is already in another match`() {
        runBlocking {
            val store = TicketStore()
            val taken = ticket()
            val free = ticket()
            store.add(taken)
            store.add(free)
            store.matched(listOf(taken.id), UUID.randomUUID())

            // This is what keeps a ticket queued for several types out of two matches.
            assertNull(store.matched(listOf(taken.id, free.id), UUID.randomUUID()))

            // The other ticket must be untouched, it keeps searching.
            assertEquals(TicketState.TICKET_STATE_SEARCHING, store.get(free.id)?.state)
            assertNull(store.get(free.id)?.matchId)
        }
    }

    @Test
    fun `matched fails when a ticket is gone`() {
        runBlocking {
            val store = TicketStore()
            val ticket = ticket()
            store.add(ticket)

            assertNull(store.matched(listOf(ticket.id, UUID.randomUUID()), UUID.randomUUID()))
            assertEquals(TicketState.TICKET_STATE_SEARCHING, store.get(ticket.id)?.state)
        }
    }

}

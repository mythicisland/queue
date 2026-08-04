package net.mythicisland.queue.runtime.ticket

import build.buf.gen.mythicisland.queue.v2.TicketState
import kotlinx.coroutines.runBlocking
import net.mythicisland.queue.runtime.NOW
import net.mythicisland.queue.runtime.players
import net.mythicisland.queue.runtime.ticket
import kotlin.test.Test
import kotlin.test.assertEquals

class TicketPoolTest {

    @Test
    fun `only returns tickets of the asked queue type`() {
        runBlocking {
            val store = TicketStore()
            val pool = TicketPool(store)
            val battle = ticket(queueTypes = listOf("battle"))
            store.add(battle)
            store.add(ticket(queueTypes = listOf("skywars")))

            assertEquals(listOf(battle), pool.searching("battle"))
        }
    }

    @Test
    fun `a ticket queued for several types shows up in every pool`() {
        runBlocking {
            val store = TicketStore()
            val pool = TicketPool(store)
            val ticket = ticket(queueTypes = listOf("battle", "skywars"))
            store.add(ticket)

            assertEquals(listOf(ticket), pool.searching("battle"))
            assertEquals(listOf(ticket), pool.searching("skywars"))
        }
    }

    @Test
    fun `leaves out tickets that are no longer searching`() {
        runBlocking {
            val store = TicketStore()
            val pool = TicketPool(store)
            store.add(ticket(state = TicketState.TICKET_STATE_MATCHED))
            store.add(ticket(state = TicketState.TICKET_STATE_ASSIGNED))

            assertEquals(emptyList(), pool.searching("battle"))
        }
    }

    @Test
    fun `returns the oldest ticket first`() {
        runBlocking {
            val store = TicketStore()
            val pool = TicketPool(store)
            val newest = ticket(createdAt = NOW.plusSeconds(20))
            val oldest = ticket(createdAt = NOW)
            val middle = ticket(createdAt = NOW.plusSeconds(10))
            store.add(newest)
            store.add(oldest)
            store.add(middle)

            assertEquals(listOf(oldest, middle, newest), pool.searching("battle"))
        }
    }

    @Test
    fun `counts every player of a party`() {
        runBlocking {
            val store = TicketStore()
            val pool = TicketPool(store)
            store.add(ticket(playerIds = players(3)))
            store.add(ticket(playerIds = players(1)))

            val stats = pool.stats("battle", activeMatches = 2)

            assertEquals("battle", stats.queueType)
            assertEquals(2, stats.searchingTickets)
            assertEquals(4, stats.searchingPlayers)
            assertEquals(2, stats.activeMatches)
        }
    }

}

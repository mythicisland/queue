package net.mythicisland.queue.runtime.repository

import build.buf.gen.mythicisland.queue.v2.MatchState
import kotlinx.coroutines.runBlocking
import net.mythicisland.queue.runtime.match
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MatchRepositoryTest {

    @Test
    fun `finds a match by any of its tickets`() {
        runBlocking {
            val matches = MatchRepository()
            val ticketIds = listOf(UUID.randomUUID(), UUID.randomUUID())
            val match = match(ticketIds = ticketIds)
            matches.add(match)

            assertEquals(match, matches.get(match.id))
            ticketIds.forEach { assertEquals(match, matches.getByTicket(it)) }
        }
    }

    @Test
    fun `filters matches by queue type and state`() {
        runBlocking {
            val matches = MatchRepository()
            val battle = match(queueType = "battle", state = MatchState.MATCH_STATE_COUNTDOWN)
            val skywars = match(queueType = "skywars")
            matches.add(battle)
            matches.add(skywars)

            assertEquals(listOf(battle), matches.getAllByType("battle"))
            assertEquals(listOf(battle), matches.getAllByState(MatchState.MATCH_STATE_COUNTDOWN))
            assertEquals(2, matches.getAll().size)
        }
    }

    @Test
    fun `dropping a ticket leaves the rest of the match alone`() {
        runBlocking {
            val matches = MatchRepository()
            val leaving = UUID.randomUUID()
            val staying = UUID.randomUUID()
            matches.add(match(ticketIds = listOf(leaving, staying)))

            val updated = matches.removeTicket(leaving)

            assertNotNull(updated)
            assertEquals(listOf(staying), updated.ticketIds)
            assertNull(matches.getByTicket(leaving))
            assertEquals(updated, matches.getByTicket(staying))
        }
    }

    @Test
    fun `dropping the last ticket leaves an empty match behind`() {
        runBlocking {
            val matches = MatchRepository()
            val only = UUID.randomUUID()
            val match = match(ticketIds = listOf(only))
            matches.add(match)

            val updated = matches.removeTicket(only)

            // The reconciler is what fails an empty match, the repository keeps it.
            assertNotNull(updated)
            assertEquals(emptyList(), updated.ticketIds)
            assertNotNull(matches.get(match.id))
        }
    }

    @Test
    fun `dropping a ticket of no match does nothing`() {
        runBlocking {
            val matches = MatchRepository()

            assertNull(matches.removeTicket(UUID.randomUUID()))
        }
    }

    @Test
    fun `removing a match clears its ticket index`() {
        runBlocking {
            val matches = MatchRepository()
            val ticketId = UUID.randomUUID()
            val match = match(ticketIds = listOf(ticketId))
            matches.add(match)

            assertEquals(match, matches.remove(match.id))
            assertNull(matches.get(match.id))
            assertNull(matches.getByTicket(ticketId))
        }
    }

    @Test
    fun `ignores an update of a match that was removed`() {
        runBlocking {
            val matches = MatchRepository()

            assertNull(matches.update(match()))
        }
    }

}

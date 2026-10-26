package net.mythicisland.queue.runtime.match

import net.mythicisland.queue.shared.ticket.Ticket
import net.mythicisland.queue.shared.ticket.TicketState
import net.mythicisland.queue.shared.type.MatchConfig
import net.mythicisland.queue.shared.type.QueueType
import net.mythicisland.queue.shared.type.ServerConfig
import net.mythicisland.queue.shared.type.TeamConfig
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TeamAssignerTest {

    @Test
    fun `returns no teams without a team config`() {
        val type = QueueType(server = ServerConfig(group = "ffa"), match = MatchConfig(minPlayers = 2, maxPlayers = 8))

        assertEquals(emptyList(), TeamAssigner.assign(type, listOf(ticket(1), ticket(1))))
    }

    @Test
    fun `balances single players across two teams`() {
        val teams = assertNotNull(TeamAssigner.assign(teams(count = 2, size = 4), List(6) { ticket(1) }))

        assertEquals(listOf(3, 3), teams.map { it.playerIds.size })
    }

    @Test
    fun `never splits a party`() {
        val party = ticket(3)
        val teams = assertNotNull(TeamAssigner.assign(teams(count = 2, size = 4), listOf(ticket(1), party, ticket(2))))

        assertTrue(teams.any { it.playerIds.containsAll(party.playerIds) })
    }

    @Test
    fun `uses more teams when parties do not fit otherwise`() {
        val teams = assertNotNull(TeamAssigner.assign(teams(count = 3, size = 3), listOf(ticket(2), ticket(2), ticket(2))))

        assertEquals(3, teams.size)
    }

    @Test
    fun `fails when the parties cannot be placed`() {
        assertNull(TeamAssigner.assign(teams(count = 2, size = 3), listOf(ticket(2), ticket(2), ticket(2))))
    }

    @Test
    fun `creates at least two teams`() {
        val teams = assertNotNull(TeamAssigner.assign(teams(count = 4, size = 4), listOf(ticket(1))))

        assertEquals(2, teams.size)
    }

    private fun teams(count: Int, size: Int): QueueType =
        QueueType(server = ServerConfig(group = "teams"), match = MatchConfig(minPlayers = 1, teams = TeamConfig(count, size)))

    private fun ticket(players: Int): Ticket = Ticket(
        id = UUID.randomUUID().toString(),
        playerIds = List(players) { UUID.randomUUID() },
        queueTypes = listOf("teams"),
        state = TicketState.SEARCHING,
        createdAt = Instant.EPOCH,
    )

}

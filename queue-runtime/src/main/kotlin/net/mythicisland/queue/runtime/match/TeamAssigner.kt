package net.mythicisland.queue.runtime.match

import net.mythicisland.queue.shared.match.Team
import net.mythicisland.queue.shared.ticket.Ticket
import net.mythicisland.queue.shared.type.QueueType
import java.util.UUID

object TeamAssigner {

    fun assign(type: QueueType, tickets: List<Ticket>): List<Team>? {
        val teams = type.match.teams ?: return emptyList()
        val players = tickets.sumOf { it.getPlayerCount() }
        val minTeams = maxOf(2, (players + teams.size - 1) / teams.size)

        return (minTeams..teams.count).firstNotNullOfOrNull { count -> distribute(tickets, count, teams.size) }
    }

    private fun distribute(tickets: List<Ticket>, count: Int, size: Int): List<Team>? {
        val teams = List(count) { mutableListOf<UUID>() }

        for (ticket in tickets.sortedByDescending { it.getPlayerCount() }) {
            val team = teams.filter { it.size + ticket.getPlayerCount() <= size }.minByOrNull { it.size } ?: return null
            team.addAll(ticket.playerIds)
        }

        return teams.map { Team(it.toList()) }
    }

}

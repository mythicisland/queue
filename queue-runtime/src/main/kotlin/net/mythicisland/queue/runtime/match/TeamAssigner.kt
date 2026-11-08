package net.mythicisland.queue.runtime.match

import net.mythicisland.queue.shared.match.Team
import net.mythicisland.queue.shared.ticket.Ticket
import net.mythicisland.queue.shared.type.QueueType

object TeamAssigner {

    fun assign(type: QueueType, tickets: List<Ticket>): List<Team>? {
        val teams = type.match.teams ?: return emptyList()
        val players = tickets.sumOf { it.getPlayerCount() }
        val minTeams = maxOf(2, (players + teams.size - 1) / teams.size)

        return (minTeams..teams.count).firstNotNullOfOrNull { count -> distribute(tickets, count, teams.size, teams.balanceBy) }
    }

    private fun distribute(tickets: List<Ticket>, count: Int, size: Int, balanceBy: String?): List<Team>? {
        val teams = List(count) { mutableListOf<Ticket>() }
        val sorted = tickets.sortedWith(
            compareByDescending<Ticket> { it.getPlayerCount() }.thenByDescending { strength(it, balanceBy) },
        )

        for (ticket in sorted) {
            val team = teams
                .filter { players(it) + ticket.getPlayerCount() <= size }
                .minWithOrNull(compareBy<List<Ticket>> { players(it) }.thenBy { team -> team.sumOf { strength(it, balanceBy) } })
                ?: return null
            team.add(ticket)
        }

        return teams.map { team -> Team(team.flatMap { it.playerIds }) }
    }

    private fun players(team: List<Ticket>): Int = team.sumOf { it.getPlayerCount() }

    private fun strength(ticket: Ticket, balanceBy: String?): Double {
        if (balanceBy == null) {
            return 0.0
        }
        val value = ticket.properties[balanceBy]?.toDoubleOrNull() ?: return 0.0
        return value * ticket.getPlayerCount()
    }

}

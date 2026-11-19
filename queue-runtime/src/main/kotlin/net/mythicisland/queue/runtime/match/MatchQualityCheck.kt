package net.mythicisland.queue.runtime.match

import net.mythicisland.queue.shared.match.Team
import net.mythicisland.queue.shared.ticket.Ticket
import net.mythicisland.queue.shared.type.QueueType
import net.mythicisland.queue.shared.type.SpreadLimit
import java.time.Duration

object MatchQualityCheck {

    fun spread(key: String, tickets: List<Ticket>): Double? {
        val values = tickets.mapNotNull { it.properties[key]?.toDoubleOrNull() }
        if (values.isEmpty()) {
            return null
        }
        return values.max() - values.min()
    }

    fun teamDifference(teams: List<Team>, tickets: List<Ticket>, balanceBy: String): Double? {
        val values = tickets.flatMap { ticket ->
            val value = ticket.properties[balanceBy]?.toDoubleOrNull() ?: 0.0
            ticket.playerIds.map { player -> player to value }
        }.toMap()
        val averages = teams.filter { it.playerIds.isNotEmpty() }
            .map { team -> team.playerIds.sumOf { values[it] ?: 0.0 } / team.playerIds.size }
        if (averages.size < 2) {
            return null
        }
        return averages.max() - averages.min()
    }

    fun fitsSpread(limits: List<SpreadLimit>, tickets: List<Ticket>, waited: Duration): Boolean =
        limits.all { limit -> (spread(limit.key, tickets) ?: 0.0) <= limit.window.at(waited) }

    fun fitsTeamBalance(type: QueueType, teams: List<Team>, tickets: List<Ticket>, waited: Duration): Boolean {
        val window = type.match.quality.teamBalance ?: return true
        val balanceBy = type.match.teams?.balanceBy ?: return true
        val difference = teamDifference(teams, tickets, balanceBy) ?: return true
        return difference <= window.at(waited)
    }

}

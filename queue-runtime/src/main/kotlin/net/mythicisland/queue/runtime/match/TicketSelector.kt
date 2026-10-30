package net.mythicisland.queue.runtime.match

import net.mythicisland.queue.shared.match.Team
import net.mythicisland.queue.shared.ticket.Ticket
import net.mythicisland.queue.shared.type.QueueType
import java.time.Instant

object TicketSelector {

    fun select(type: QueueType, searching: List<Ticket>, now: Instant): TicketSelection? {
        val config = type.match
        val selected = mutableListOf<Ticket>()
        val teams = mutableListOf<Team>()

        for (ticket in searching.sortedBy { it.createdAt }) {
            val candidate = selected + ticket
            if (players(candidate) > config.effectiveMaxPlayers) {
                continue
            }
            val assigned = TeamAssigner.assign(type, candidate) ?: continue
            selected.add(ticket)
            teams.clear()
            teams.addAll(assigned)
        }

        val players = players(selected)
        if (players < config.minPlayers) {
            return null
        }
        val selection = TicketSelection(selected.toList(), teams.toList())
        if (players == config.effectiveMaxPlayers) {
            return selection
        }

        val playerCounts = selected.runningFold(0) { sum, ticket -> sum + ticket.getPlayerCount() }.drop(1)
        val reachedMinAt = selected[playerCounts.indexOfFirst { it >= config.minPlayers }].createdAt

        return if (now.isBefore(reachedMinAt.plus(config.fillTime))) null else selection
    }

    private fun players(tickets: List<Ticket>): Int = tickets.sumOf { it.getPlayerCount() }

}

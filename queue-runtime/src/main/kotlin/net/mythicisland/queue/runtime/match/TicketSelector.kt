package net.mythicisland.queue.runtime.match

import net.mythicisland.queue.shared.ticket.Ticket
import net.mythicisland.queue.shared.type.QueueType
import java.time.Instant

object TicketSelector {

    fun select(type: QueueType, searching: List<Ticket>, now: Instant): List<Ticket>? {
        val config = type.match
        val selected = mutableListOf<Ticket>()

        for (ticket in searching.sortedBy { it.createdAt }) {
            val candidate = selected + ticket
            if (players(candidate) <= config.effectiveMaxPlayers && TeamAssigner.assign(type, candidate) != null) {
                selected.add(ticket)
            }
        }

        val players = players(selected)
        if (players < config.minPlayers) {
            return null
        }
        if (players == config.effectiveMaxPlayers) {
            return selected
        }

        val playerCounts = selected.runningFold(0) { sum, ticket -> sum + ticket.getPlayerCount() }.drop(1)
        val reachedMinAt = selected[playerCounts.indexOfFirst { it >= config.minPlayers }].createdAt

        return if (now.isBefore(reachedMinAt.plus(config.fillTime))) null else selected
    }

    private fun players(tickets: List<Ticket>): Int = tickets.sumOf { it.getPlayerCount() }

}

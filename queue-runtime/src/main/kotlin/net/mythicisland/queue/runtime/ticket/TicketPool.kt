package net.mythicisland.queue.runtime.ticket

import build.buf.gen.mythicisland.queue.v2.TicketState
import net.mythicisland.queue.shared.match.Ticket
import net.mythicisland.queue.shared.queue.QueueStats

class TicketPool(
    private val tickets: TicketStore,
) {

    fun getAllTickets(queueType: String): List<Ticket> {
        return tickets.getAll()
            .filter { it.state == TicketState.TICKET_STATE_SEARCHING && queueType in it.queueTypes }
            .sortedBy { it.createdAt }
    }

    fun getQueueStats(queueType: String, activeMatches: Int): QueueStats {
        val searching = getAllTickets(queueType)

        return QueueStats(
            queueType = queueType,
            searchingTickets = searching.size,
            searchingPlayers = searching.sumOf { it.playerIds.size },
            activeMatches = activeMatches,
        )
    }

}

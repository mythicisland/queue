package net.mythicisland.queue.runtime.ticket

import build.buf.gen.mythicisland.queue.v2.TicketState
import net.mythicisland.queue.shared.match.Ticket
import net.mythicisland.queue.shared.queue.QueueStats

/**
 * The view the matchmaker works on: the tickets that are still searching,
 * grouped by queue type.
 *
 * A ticket searching in several queue types shows up in every one of their
 * pools until it lands in a match.
 *
 * @param tickets the store to read the tickets from.
 */
class TicketPool(
    private val tickets: TicketStore,
) {

    /**
     * All tickets searching in a queue type, oldest first.
     *
     * @param queueType the name of the queue type.
     */
    fun searching(queueType: String): List<Ticket> {
        return tickets.getAll()
            .filter { it.state == TicketState.TICKET_STATE_SEARCHING && queueType in it.queueTypes }
            .sortedBy { it.createdAt }
    }

    /**
     * Builds the live numbers of a queue type.
     *
     * @param queueType the name of the queue type.
     * @param activeMatches the amount of matches of that type that did not finish yet.
     */
    fun stats(queueType: String, activeMatches: Int): QueueStats {
        val searching = searching(queueType)

        return QueueStats(
            queueType = queueType,
            searchingTickets = searching.size,
            searchingPlayers = searching.sumOf { it.playerCount },
            activeMatches = activeMatches,
        )
    }

}

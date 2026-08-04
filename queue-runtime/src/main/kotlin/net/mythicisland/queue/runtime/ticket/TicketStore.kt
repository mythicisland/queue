package net.mythicisland.queue.runtime.ticket

import build.buf.gen.mythicisland.queue.v2.TicketState
import net.mythicisland.queue.shared.match.Ticket
import org.apache.logging.log4j.LogManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds every ticket that is currently in matchmaking.
 *
 * Reads go straight to the maps, writes that touch more than one map are
 * guarded by a lock so a ticket and its player index can never drift apart.
 */
class TicketStore {

    private val logger = LogManager.getLogger(TicketStore::class.java)

    private val lock = Any()
    private val tickets = ConcurrentHashMap<UUID, Ticket>()
    private val playerToTicket = ConcurrentHashMap<UUID, UUID>()

    /**
     * Gets a ticket by its id.
     */
    fun get(id: UUID): Ticket? {
        return tickets[id]
    }

    /**
     * Gets the ticket a player belongs to.
     */
    fun getByPlayer(playerId: UUID): Ticket? {
        return playerToTicket[playerId]?.let { tickets[it] }
    }

    /**
     * Gets every ticket currently in matchmaking.
     */
    fun getAll(): List<Ticket> {
        return tickets.values.toList()
    }

    /**
     * Gets the tickets for the given ids, skipping the ones that are gone.
     */
    fun getAll(ids: Collection<UUID>): List<Ticket> {
        return ids.mapNotNull { tickets[it] }
    }

    /**
     * Adds a ticket, unless one of its players is queued already.
     *
     * @return true if the ticket was added.
     */
    fun add(ticket: Ticket): Boolean {
        synchronized(lock) {
            val queued = ticket.playerIds.filter { playerToTicket.containsKey(it) }
            if (queued.isNotEmpty()) {
                logger.debug("Rejected ticket {}, players {} are already queued", ticket.id, queued)
                return false
            }

            tickets[ticket.id] = ticket
            ticket.playerIds.forEach { playerToTicket[it] = ticket.id }
            return true
        }
    }

    /**
     * Replaces a ticket with an updated copy.
     *
     * @return the stored ticket, or null if it was removed in the meantime.
     */
    fun update(ticket: Ticket): Ticket? {
        synchronized(lock) {
            if (!tickets.containsKey(ticket.id)) {
                logger.debug("Skipped update of ticket {}, it is no longer stored", ticket.id)
                return null
            }

            tickets[ticket.id] = ticket
            return ticket
        }
    }

    /**
     * Removes a ticket and frees its players.
     *
     * @return the removed ticket, or null if it was not stored.
     */
    fun remove(id: UUID): Ticket? {
        synchronized(lock) {
            val ticket = tickets.remove(id) ?: return null
            ticket.playerIds.forEach { playerToTicket.remove(it, id) }
            return ticket
        }
    }

    /**
     * Moves the given tickets into a match, but only if every one of them is
     * still searching. This is what keeps a ticket that is queued for several
     * queue types from ending up in two matches at once.
     *
     * @return the updated tickets, or null if one of them was not searching anymore.
     */
    fun matched(ids: List<UUID>, matchId: UUID): List<Ticket>? {
        synchronized(lock) {
            val found = ids.map { tickets[it] ?: return null }
            if (found.any { it.state != TicketState.TICKET_STATE_SEARCHING }) return null

            val updated = found.map {
                it.copy(state = TicketState.TICKET_STATE_MATCHED, matchId = matchId)
            }
            updated.forEach { tickets[it.id] = it }
            return updated
        }
    }

}

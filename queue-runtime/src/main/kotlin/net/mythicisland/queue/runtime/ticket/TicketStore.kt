package net.mythicisland.queue.runtime.ticket

import build.buf.gen.mythicisland.queue.v2.TicketState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mythicisland.queue.shared.match.Ticket
import org.apache.logging.log4j.LogManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds every ticket that is currently in matchmaking.
 */
class TicketStore {

    private val logger = LogManager.getLogger(TicketStore::class.java)

    private val mutex = Mutex()
    private val tickets = ConcurrentHashMap<UUID, Ticket>()
    private val assignments = ConcurrentHashMap<UUID, UUID>()

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
        return assignments[playerId]?.let { tickets[it] }
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
    suspend fun add(ticket: Ticket): Boolean {
        mutex.withLock {
            val queued = ticket.playerIds.filter { assignments.containsKey(it) }
            if (queued.isNotEmpty()) {
                logger.debug("Rejected ticket {}, players {} are already queued", ticket.id, queued)
                return false
            }

            tickets[ticket.id] = ticket
            ticket.playerIds.forEach { assignments[it] = ticket.id }
            return true
        }
    }

    /**
     * Replaces a ticket with an updated copy.
     *
     * @return the stored ticket, or null if it was removed in the meantime.
     */
    suspend fun update(ticket: Ticket): Ticket? {
        mutex.withLock {
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
    suspend fun remove(id: UUID): Ticket? {
        mutex.withLock {
            val ticket = tickets.remove(id) ?: return null
            ticket.playerIds.forEach { assignments.remove(it, id) }
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
    suspend fun matched(ids: List<UUID>, matchId: UUID): List<Ticket>? {
        mutex.withLock {
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

package net.mythicisland.queue.runtime.ticket

import build.buf.gen.mythicisland.queue.v2.TicketState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mythicisland.queue.shared.match.Ticket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TicketStore {

    private val mutex = Mutex()
    private val tickets = ConcurrentHashMap<UUID, Ticket>()
    private val assignments = ConcurrentHashMap<UUID, UUID>()

    fun get(id: UUID): Ticket? {
        return tickets[id]
    }

    fun getByPlayer(playerId: UUID): Ticket? {
        return assignments[playerId]?.let { tickets[it] }
    }

    fun getAll(): List<Ticket> {
        return tickets.values.toList()
    }

    fun getAll(ids: Collection<UUID>): List<Ticket> {
        return ids.mapNotNull { tickets[it] }
    }

    suspend fun add(ticket: Ticket): Boolean {
        mutex.withLock {
            val playerIds = ticket.playerIds.filter { assignments.containsKey(it) }
            if (playerIds.isNotEmpty()) return false
            tickets[ticket.id] = ticket
            ticket.playerIds.forEach { assignments[it] = ticket.id }
            return true
        }
    }

    suspend fun update(ticket: Ticket): Ticket? {
        mutex.withLock {
            if (!tickets.containsKey(ticket.id)) return null
            tickets[ticket.id] = ticket
            return ticket
        }
    }

    suspend fun remove(id: UUID): Ticket? {
        mutex.withLock {
            val ticket = tickets.remove(id) ?: return null
            ticket.playerIds.forEach { assignments.remove(it, id) }
            return ticket
        }
    }

    suspend fun matched(ids: List<UUID>, matchId: UUID): List<Ticket>? {
        mutex.withLock {
            val activeTickets = ids.map { tickets[it] ?: return null }
            if (activeTickets.any { it.state != TicketState.TICKET_STATE_SEARCHING }) return null

            val updatedTickets = activeTickets.map {
                it.copy(state = TicketState.TICKET_STATE_MATCHED, matchId = matchId)
            }
            updatedTickets.forEach {
                tickets[it.id] = it
            }
            return updatedTickets
        }
    }

}

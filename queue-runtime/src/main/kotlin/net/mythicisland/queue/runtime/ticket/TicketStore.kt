package net.mythicisland.queue.runtime.ticket

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mythicisland.queue.shared.ticket.Ticket
import net.mythicisland.queue.shared.ticket.TicketState
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TicketStore {

    private val mutex = Mutex()
    private val tickets = ConcurrentHashMap<String, Ticket>()
    private val ticketsByPlayer = ConcurrentHashMap<UUID, String>()

    fun get(id: String): Ticket? = tickets[id]

    fun getByPlayer(player: UUID): Ticket? {
        val id = ticketsByPlayer[player] ?: return null
        return tickets[id]
    }

    fun all(): List<Ticket> = tickets.values.toList()

    fun searching(queueType: String): List<Ticket> =
        tickets.values.filter { it.state == TicketState.SEARCHING && queueType in it.queueTypes }

    suspend fun add(ticket: Ticket): Boolean = mutex.withLock {
        if (ticket.playerIds.any { ticketsByPlayer.containsKey(it) }) {
            return@withLock false
        }

        tickets[ticket.id] = ticket
        for (player in ticket.playerIds) {
            ticketsByPlayer[player] = ticket.id
        }
        true
    }

    suspend fun remove(id: String): Ticket? = mutex.withLock {
        val ticket = tickets.remove(id) ?: return@withLock null
        for (player in ticket.playerIds) {
            ticketsByPlayer.remove(player, id)
        }
        ticket
    }

    suspend fun replace(previous: Ticket, current: Ticket): Boolean = mutex.withLock {
        tickets.replace(previous.id, previous, current)
    }

    suspend fun claim(ids: List<String>, matchId: String): List<Ticket>? = mutex.withLock {
        val searching = ids.mapNotNull { tickets[it] }.filter { it.state == TicketState.SEARCHING }
        if (searching.size != ids.size) {
            return@withLock null
        }

        val claimed = searching.map { it.copy(state = TicketState.MATCHED, matchId = matchId) }
        for (ticket in claimed) {
            tickets[ticket.id] = ticket
        }
        claimed
    }

}

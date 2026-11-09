package net.mythicisland.queue.velocity.ticket

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.mythicisland.queue.api.event.Subscription
import net.mythicisland.queue.api.event.ticket.TicketEventApi
import net.mythicisland.queue.api.ticket.Ticket
import net.mythicisland.queue.api.ticket.TicketState
import net.mythicisland.queue.velocity.Colors
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TicketTracker(private val server: ProxyServer) {

    private val tickets = ConcurrentHashMap<UUID, Ticket>()

    fun tickets(): Map<UUID, Ticket> = tickets

    fun subscribe(events: TicketEventApi): List<Subscription> = listOf(
        events.onCreated { event -> track(event.ticket) },
        events.onUpdated { event -> track(event.ticket) },
        events.onStateChanged { event ->
            track(event.ticket)
            if (event.ticket.state == TicketState.SEARCHING && event.previousState != TicketState.SEARCHING) {
                send(event.ticket, Component.text("Your match was cancelled, searching again.", Colors.TEXT))
            }
        },
        events.onDeleted { event -> untrack(event.ticket) },
    )

    fun load(tickets: List<Ticket>) {
        for (ticket in tickets) {
            for (playerId in localPlayers(ticket)) {
                this.tickets.putIfAbsent(playerId, ticket)
            }
        }
    }

    fun track(ticket: Ticket) {
        for (playerId in localPlayers(ticket)) {
            tickets[playerId] = ticket
        }
    }

    fun untrack(ticket: Ticket) {
        for (playerId in ticket.playerIds) {
            val current = tickets[playerId] ?: continue
            if (current.id == ticket.id && tickets.remove(playerId, current)) {
                clearActionBar(playerId)
            }
        }
    }

    fun untrackPlayer(playerId: UUID) {
        if (tickets.remove(playerId) != null) {
            clearActionBar(playerId)
        }
    }

    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        tickets.remove(event.player.uniqueId)
    }

    private fun localPlayers(ticket: Ticket): List<UUID> =
        ticket.playerIds.filter { playerId -> server.getPlayer(playerId).isPresent }

    private fun send(ticket: Ticket, message: Component) {
        for (playerId in ticket.playerIds) {
            val player = server.getPlayer(playerId).orElse(null) ?: continue
            player.sendMessage(message)
        }
    }

    private fun clearActionBar(playerId: UUID) {
        val player = server.getPlayer(playerId).orElse(null) ?: return
        player.sendActionBar(Component.empty())
    }

}

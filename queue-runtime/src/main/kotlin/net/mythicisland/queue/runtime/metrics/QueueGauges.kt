package net.mythicisland.queue.runtime.metrics

import app.simplecloud.api.server.ServerState
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import net.mythicisland.queue.runtime.match.MatchStore
import net.mythicisland.queue.runtime.metrics.MetricAttributes.KIND
import net.mythicisland.queue.runtime.metrics.MetricAttributes.QUEUE_TYPE
import net.mythicisland.queue.runtime.metrics.MetricAttributes.SERVER_GROUP
import net.mythicisland.queue.runtime.metrics.MetricAttributes.STATE
import net.mythicisland.queue.runtime.server.ServerRegistry
import net.mythicisland.queue.runtime.ticket.TicketStore
import net.mythicisland.queue.runtime.type.QueueTypeRegistry
import net.mythicisland.queue.shared.match.MatchState
import net.mythicisland.queue.shared.ticket.TicketState
import java.time.Duration
import java.time.Instant

class QueueGauges(private val meter: Meter) {

    private val tickets = meter.gaugeBuilder("queue.tickets")
        .setDescription("Tickets in the queue")
        .setUnit("{ticket}")
        .ofLongs()
        .buildObserver()

    private val searchingTickets = meter.gaugeBuilder("queue.tickets.searching")
        .setDescription("Tickets that are searching for a match")
        .setUnit("{ticket}")
        .ofLongs()
        .buildObserver()

    private val searchingPlayers = meter.gaugeBuilder("queue.players.searching")
        .setDescription("Players that are searching for a match")
        .setUnit("{player}")
        .ofLongs()
        .buildObserver()

    private val oldestTicket = meter.gaugeBuilder("queue.ticket.oldest_age")
        .setDescription("How long the oldest searching ticket has been waiting")
        .setUnit("s")
        .buildObserver()

    private val matches = meter.gaugeBuilder("queue.matches")
        .setDescription("Matches the queue keeps track of")
        .setUnit("{match}")
        .ofLongs()
        .buildObserver()

    private val servers = meter.gaugeBuilder("queue.servers")
        .setDescription("Servers of the queue groups")
        .setUnit("{server}")
        .ofLongs()
        .buildObserver()

    private val capacity = meter.gaugeBuilder("queue.server.capacity")
        .setDescription("Match slots of shared servers")
        .setUnit("{match}")
        .ofLongs()
        .buildObserver()

    fun observe(
        types: QueueTypeRegistry,
        ticketStore: TicketStore,
        matchStore: MatchStore,
        registry: ServerRegistry,
    ): AutoCloseable = meter.batchCallback(
        { record(types, ticketStore, matchStore, registry) },
        tickets,
        searchingTickets,
        searchingPlayers,
        oldestTicket,
        matches,
        servers,
        capacity,
    )

    private fun record(types: QueueTypeRegistry, ticketStore: TicketStore, matchStore: MatchStore, registry: ServerRegistry) {
        val now = Instant.now()
        val allTickets = ticketStore.all()
        val allMatches = matchStore.all()

        for (state in TicketState.entries) {
            tickets.record(allTickets.count { it.state == state }.toLong(), Attributes.of(STATE, state.name.lowercase()))
        }

        for (type in types.all()) {
            val attributes = Attributes.of(QUEUE_TYPE, type.name)
            val searching = allTickets.filter { it.state == TicketState.SEARCHING && type.name in it.queueTypes }
            searchingTickets.record(searching.size.toLong(), attributes)
            searchingPlayers.record(searching.sumOf { it.getPlayerCount() }.toLong(), attributes)

            val oldest = searching.minOfOrNull { it.createdAt }
            val age = if (oldest == null) 0.0 else Duration.between(oldest, now).toMillis() / 1000.0
            oldestTicket.record(age, attributes)

            for (state in TRACKED_MATCH_STATES) {
                val count = allMatches.count { it.queueType == type.name && it.state == state }
                matches.record(count.toLong(), Attributes.of(QUEUE_TYPE, type.name, STATE, state.name.lowercase()))
            }
        }

        for (group in types.groups()) {
            val groupServers = registry.byGroup(group)
            for (state in TRACKED_SERVER_STATES) {
                val count = groupServers.count { it.state == state }
                servers.record(count.toLong(), Attributes.of(SERVER_GROUP, group, STATE, state.name.lowercase()))
            }

            if (types.isShared(group)) {
                val total = groupServers.sumOf { it.capacity ?: 0 }
                val used = groupServers.sumOf { it.matches.size }
                capacity.record(total.toLong(), Attributes.of(SERVER_GROUP, group, KIND, "total"))
                capacity.record(used.toLong(), Attributes.of(SERVER_GROUP, group, KIND, "used"))
            }
        }
    }

    private companion object {
        val TRACKED_MATCH_STATES = MatchState.entries - setOf(MatchState.FAILED, MatchState.CANCELLED)
        val TRACKED_SERVER_STATES = ServerState.entries - ServerRegistry.STOPPED_STATES - ServerState.UNKNOWN_STATE
    }

}

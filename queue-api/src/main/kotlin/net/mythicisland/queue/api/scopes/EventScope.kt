package net.mythicisland.queue.api.scopes

import net.mythicisland.queue.api.QueueDsl
import net.mythicisland.queue.api.event.EventApi
import net.mythicisland.queue.api.event.Subscription
import net.mythicisland.queue.api.event.match.MatchCreatedEvent
import net.mythicisland.queue.api.event.match.MatchStateChangedEvent
import net.mythicisland.queue.api.event.match.MatchTransferredEvent
import net.mythicisland.queue.api.event.ticket.TicketCreatedEvent
import net.mythicisland.queue.api.event.ticket.TicketDeletedEvent
import net.mythicisland.queue.api.event.ticket.TicketStateChangedEvent
import net.mythicisland.queue.api.match.MatchState
import net.mythicisland.queue.api.ticket.TicketState

/**
 * Collects event handlers and hands back a single [Subscription] for all of them.
 *
 * Registering one listener at a time means keeping one handle per listener
 * around. In a plugin that usually ends in a forgotten unsubscribe on disable,
 * so this scope bundles them.
 *
 * ```
 * val subscription = api.events {
 *     onTicketStateChanged(to = TicketState.ASSIGNED) { event ->
 *         connect(event.ticket)
 *     }
 *     onMatchTransferred { event ->
 *         logger.info("${event.transferredPlayerIds.size} players moved")
 *     }
 * }
 *
 * // on disable
 * subscription.unsubscribe()
 * ```
 *
 * @param events the event API to register on.
 */
@QueueDsl
class EventScope internal constructor(
    private val events: EventApi,
) {

    private val subscriptions = mutableListOf<Subscription>()

    /**
     * Called when a player or party entered matchmaking.
     */
    fun onTicketCreated(handler: (TicketCreatedEvent) -> Unit) {
        subscriptions.add(events.ticket().onCreated(handler))
    }

    /**
     * Called when a ticket moved to a new state.
     *
     * @param to only call the handler when the ticket reached this state, null for every change.
     */
    fun onTicketStateChanged(to: TicketState? = null, handler: (TicketStateChangedEvent) -> Unit) {
        subscriptions.add(events.ticket().onStateChanged { event ->
            if (to == null || event.ticket().state() == to) handler(event)
        })
    }

    /**
     * Called when a ticket left matchmaking, no matter for which reason.
     */
    fun onTicketDeleted(handler: (TicketDeletedEvent) -> Unit) {
        subscriptions.add(events.ticket().onDeleted(handler))
    }

    /**
     * Called when enough tickets were found to form a match.
     */
    fun onMatchCreated(handler: (MatchCreatedEvent) -> Unit) {
        subscriptions.add(events.match().onCreated(handler))
    }

    /**
     * Called when a match moved to a new state.
     *
     * @param to only call the handler when the match reached this state, null for every change.
     */
    fun onMatchStateChanged(to: MatchState? = null, handler: (MatchStateChangedEvent) -> Unit) {
        subscriptions.add(events.match().onStateChanged { event ->
            if (to == null || event.match().state() == to) handler(event)
        })
    }

    /**
     * Called after the players of a match were sent to their game server.
     */
    fun onMatchTransferred(handler: (MatchTransferredEvent) -> Unit) {
        subscriptions.add(events.match().onTransferred(handler))
    }

    internal fun subscription(): Subscription = CompositeSubscription(subscriptions.toList())

}

/**
 * Unsubscribes every listener that was registered in one [EventScope].
 */
private class CompositeSubscription(
    private val subscriptions: List<Subscription>,
) : Subscription {

    override fun unsubscribe() {
        subscriptions.forEach { it.unsubscribe() }
    }

}

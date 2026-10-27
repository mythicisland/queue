package net.mythicisland.queue.api.event.ticket

import net.mythicisland.queue.api.event.Subscription
import java.util.function.Consumer

/**
 * Subscribing to ticket events.
 */
interface TicketEventApi {

    /**
     * Calls [handler] when a ticket was created.
     */
    fun onCreated(handler: Consumer<TicketCreatedEvent>): Subscription

    /**
     * Calls [handler] when a ticket changed its state.
     */
    fun onStateChanged(handler: Consumer<TicketStateChangedEvent>): Subscription

    /**
     * Calls [handler] when the properties of a ticket changed.
     */
    fun onUpdated(handler: Consumer<TicketUpdatedEvent>): Subscription

    /**
     * Calls [handler] when a ticket was deleted.
     */
    fun onDeleted(handler: Consumer<TicketDeletedEvent>): Subscription

}

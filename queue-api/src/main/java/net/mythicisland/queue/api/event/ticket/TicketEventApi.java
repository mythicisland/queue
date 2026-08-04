package net.mythicisland.queue.api.event.ticket;

import net.mythicisland.queue.api.event.Subscription;

import java.util.function.Consumer;

/**
 * API for subscribing to ticket lifecycle events.
 */
public interface TicketEventApi {

    /**
     * Subscribes to events triggered when a player or party entered matchmaking.
     *
     * @param handler a consumer that will process the ticket creation events
     * @return a subscription handle to manage the listener lifecycle
     */
    Subscription onCreated(Consumer<TicketCreatedEvent> handler);

    /**
     * Subscribes to events triggered when a ticket moves to a new state.
     *
     * @param handler a consumer that will process the state change events
     * @return a subscription handle to manage the listener lifecycle
     */
    Subscription onStateChanged(Consumer<TicketStateChangedEvent> handler);

    /**
     * Subscribes to events triggered when a ticket leaves matchmaking.
     *
     * @param handler a consumer that will process the ticket deletion events
     * @return a subscription handle to manage the listener lifecycle
     */
    Subscription onDeleted(Consumer<TicketDeletedEvent> handler);

}

package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.event.Subscription;

import java.util.function.Consumer;

/**
 * API for subscribing to queue lifecycle and status events.
 */
public interface QueueEventApi {

    /**
     * Subscribes to events triggered when a new queue is created.
     *
     * @param handler a consumer that will process the queue creation events
     * @return a subscription handle to manage the listener lifecycle
     */
    Subscription onCreated(Consumer<QueueCreatedEvent> handler);

    /**
     * Subscribes to events triggered when a queue is deleted (usually after finishing).
     *
     * @param handler a consumer that will process the queue deletion events
     * @return a subscription handle to manage the listener lifecycle
     */
    Subscription onDeleted(Consumer<QueueDeletedEvent> handler);

    /**
     * Subscribes to events triggered when a game server is assigned to a queue.
     *
     * @param handler a consumer that will process the server assignment events
     * @return a subscription handle to manage the listener lifecycle
     */
    Subscription onServerAssigned(Consumer<QueueServerAssignedEvent> handler);

    /**
     * Subscribes to events triggered when a queue's status changes.
     *
     * @param handler a consumer that will process the status update events
     * @return a subscription handle to manage the listener lifecycle
     */
    Subscription onStatusUpdated(Consumer<QueueStatusUpdatedEvent> handler);

    /**
     * Subscribes to events triggered when players are transferred between queues.
     *
     * @param handler a consumer that will process the transfer events
     * @return a subscription handle to manage the listener lifecycle
     */
    Subscription onTransfer(Consumer<QueueTransferEvent> handler);

    /**
     * Subscribes to general queue update events.
     *
     * @param handler a consumer that will process the queue update events
     * @return a subscription handle to manage the listener lifecycle
     */
    Subscription onUpdated(Consumer<QueueUpdatedEvent> handler);

}

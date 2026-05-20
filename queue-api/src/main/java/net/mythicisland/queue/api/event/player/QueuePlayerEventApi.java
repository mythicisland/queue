package net.mythicisland.queue.api.event.player;

import net.mythicisland.queue.api.event.Subscription;

import java.util.function.Consumer;

/**
 * API for subscribing to player-specific queue events.
 */
public interface QueuePlayerEventApi {

    /**
     * Subscribes to events triggered when players are added to a queue.
     *
     * @param handler a consumer that will process the enqueue events
     * @return a subscription handle to manage the listener lifecycle
     */
    Subscription onEnqueued(Consumer<EnqueueEvent> handler);

    /**
     * Subscribes to events triggered when players are removed from a queue.
     *
     * @param handler a consumer that will process the dequeue events
     * @return a subscription handle to manage the listener lifecycle
     */
    Subscription onDequeued(Consumer<DequeueEvent> handler);

}

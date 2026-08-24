package net.mythicisland.queue.api.event.match;

import net.mythicisland.queue.api.event.Subscription;

import java.util.function.Consumer;

/**
 * API for subscribing to match events.
 */
public interface MatchEventApi {

    /**
     * Subscribes to events triggered when enough tickets were found for a match.
     *
     * @param handler a consumer that will process the match creation events
     * @return a subscription handle to manage the listener lifecycle
     */
    Subscription onCreated(Consumer<MatchCreatedEvent> handler);

    /**
     * Subscribes to events triggered when a match moves to a new state.
     *
     * @param handler a consumer that will process the state change events
     * @return a subscription handle to manage the listener lifecycle
     */
    Subscription onStateChanged(Consumer<MatchStateChangedEvent> handler);

    /**
     * Subscribes to events triggered after the players of a match were sent to their server.
     *
     * @param handler a consumer that will process the transfer events
     * @return a subscription handle to manage the listener lifecycle
     */
    Subscription onTransferred(Consumer<MatchTransferredEvent> handler);

}

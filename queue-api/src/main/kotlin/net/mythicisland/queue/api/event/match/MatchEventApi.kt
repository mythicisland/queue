package net.mythicisland.queue.api.event.match

import net.mythicisland.queue.api.event.Subscription
import java.util.function.Consumer

/**
 * Subscribing to match events.
 */
interface MatchEventApi {

    /**
     * Calls [handler] when a match was created.
     */
    fun onCreated(handler: Consumer<MatchCreatedEvent>): Subscription

    /**
     * Calls [handler] when a match changed its state.
     */
    fun onStateChanged(handler: Consumer<MatchStateChangedEvent>): Subscription

    /**
     * Calls [handler] when the players of a match were transferred.
     */
    fun onTransferred(handler: Consumer<MatchTransferredEvent>): Subscription

    /**
     * Calls [handler] when a match was released.
     */
    fun onReleased(handler: Consumer<MatchReleasedEvent>): Subscription

}

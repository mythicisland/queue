package net.mythicisland.queue.api.event.type

import net.mythicisland.queue.api.event.Subscription
import java.util.function.Consumer

/**
 * Subscribing to queue type events, no matter if the change came from a file or through the API.
 */
interface QueueTypeEventApi {

    /**
     * Calls [handler] when a queue type was created.
     */
    fun onCreated(handler: Consumer<QueueTypeCreatedEvent>): Subscription

    /**
     * Calls [handler] when a queue type was changed, including when it was activated or deactivated.
     */
    fun onUpdated(handler: Consumer<QueueTypeUpdatedEvent>): Subscription

    /**
     * Calls [handler] when a queue type was deleted.
     */
    fun onDeleted(handler: Consumer<QueueTypeDeletedEvent>): Subscription

}

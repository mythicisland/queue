package net.mythicisland.queue.api.internal.event

import io.nats.client.Connection
import io.nats.client.Dispatcher
import net.mythicisland.queue.api.event.Subscription

internal class SubscriptionImpl(
    private val connection: Connection,
    private val dispatcher: Dispatcher,
) : Subscription {

    override fun unsubscribe() {
        connection.closeDispatcher(dispatcher)
    }

}

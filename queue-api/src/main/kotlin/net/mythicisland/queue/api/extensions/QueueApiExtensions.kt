package net.mythicisland.queue.api.extensions

import net.mythicisland.queue.api.QueueApi
import net.mythicisland.queue.api.builders.QueueApiOptionsBuilder
import net.mythicisland.queue.api.event.Subscription
import net.mythicisland.queue.api.scopes.EventScope

/**
 * Creates a [QueueApi] from a Kotlin block.
 *
 * Without a block the options come straight from the environment variables,
 * the same as [QueueApi.create].
 *
 * ```
 * val api = queueApi {
 *     grpcHost = "queue.internal"
 *     grpcPort = 4564
 * }
 * ```
 *
 * @param block configures the options.
 * @return the connected API, close it when you are done with it.
 */
fun queueApi(block: QueueApiOptionsBuilder.() -> Unit = {}): QueueApi {
    return QueueApi.create(QueueApiOptionsBuilder().apply(block).build())
}

/**
 * Registers several event handlers at once.
 *
 * @param block registers the handlers.
 * @return one subscription that unsubscribes all of them.
 */
fun QueueApi.events(block: EventScope.() -> Unit): Subscription {
    return EventScope(event()).apply(block).subscription()
}

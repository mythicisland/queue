package net.mythicisland.queue.api.scopes

import net.mythicisland.queue.api.event.Subscription
import net.mythicisland.queue.api.event.player.DequeueEvent
import net.mythicisland.queue.api.event.player.EnqueueEvent
import net.mythicisland.queue.api.event.player.QueuePlayerEventApi

/**
 * DSL scope for subscribing to player queue events.
 *
 * ```kotlin
 * api.event().events {
 *     player {
 *         onEnqueued { println("${it.playerIds()} joined") }
 *         onDequeued { println("${it.playerIds()} left") }
 *     }
 * }
 * ```
 *
 * @see EventScope
 */
class PlayerEventScope(private val api: QueuePlayerEventApi) {

    /** All subscriptions created within this scope. */
    private val _subscriptions = mutableListOf<Subscription>()

    /** The subscriptions created by this scope, for bulk management. */
    val subscriptions: List<Subscription> get() = _subscriptions

    /**
     * Subscribes to [EnqueueEvent]s.
     *
     * @param handler called when players are added to a queue
     */
    fun onEnqueued(handler: (EnqueueEvent) -> Unit) {
        _subscriptions.add(api.onEnqueued(handler))
    }

    /**
     * Subscribes to [DequeueEvent]s.
     *
     * @param handler called when players are removed from a queue
     */
    fun onDequeued(handler: (DequeueEvent) -> Unit) {
        _subscriptions.add(api.onDequeued(handler))
    }
}

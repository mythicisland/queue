package net.mythicisland.queue.api.scopes

import net.mythicisland.queue.api.event.EventApi
import net.mythicisland.queue.api.event.Subscription

/**
 * Top-level DSL scope for subscribing to queue events.
 *
 * Collects all subscriptions from nested [queue] and [player] blocks,
 * making it easy to subscribe to multiple events at once and manage
 * their lifecycle together.
 *
 * ```kotlin
 * val subscriptions = api.event().events {
 *     queue {
 *         onCreated { println("Queue ${it.queueId()} created") }
 *         onStatusUpdated { println("${it.oldStatus()} -> ${it.newStatus()}") }
 *         onTransfer { println("${it.transferredPlayerIds().size} players transferred") }
 *     }
 *     player {
 *         onEnqueued { println("${it.playerIds()} joined ${it.queueType()}") }
 *         onDequeued { println("${it.playerIds()} left") }
 *     }
 * }
 *
 * // Later, cancel all subscriptions at once
 * subscriptions.forEach { it.unsubscribe() }
 * ```
 */
class EventScope(private val api: EventApi) {

    /** All subscriptions created within this scope and its nested scopes. */
    private val _subscriptions = mutableListOf<Subscription>()

    /** The subscriptions created by this scope, for bulk management. */
    val subscriptions: List<Subscription> get() = _subscriptions

    /**
     * Opens a [QueueEventScope] for subscribing to queue lifecycle events.
     *
     * @param block the configuration block for queue event subscriptions
     */
    fun queue(block: QueueEventScope.() -> Unit) {
        val scope = QueueEventScope(api.queue()).apply(block)
        _subscriptions.addAll(scope.subscriptions)
    }

    /**
     * Opens a [PlayerEventScope] for subscribing to player queue events.
     *
     * @param block the configuration block for player event subscriptions
     */
    fun player(block: PlayerEventScope.() -> Unit) {
        val scope = PlayerEventScope(api.player()).apply(block)
        _subscriptions.addAll(scope.subscriptions)
    }
}

/**
 * Subscribes to queue events using a Kotlin DSL.
 *
 * Returns all created subscriptions for lifecycle management.
 *
 * ```kotlin
 * val subscriptions = api.event().events {
 *     queue {
 *         onCreated { println("Queue ${it.queueId()} created") }
 *     }
 *     player {
 *         onEnqueued { println("${it.playerIds()} joined") }
 *     }
 * }
 * ```
 *
 * @param block the event subscription DSL block
 * @return all subscriptions created within the block
 */
fun EventApi.events(block: EventScope.() -> Unit): List<Subscription> {
    return EventScope(this).apply(block).subscriptions
}

package net.mythicisland.queue.api.scopes

import net.mythicisland.queue.api.event.Subscription
import net.mythicisland.queue.api.event.queue.*

/**
 * DSL scope for subscribing to queue lifecycle events.
 *
 * ```kotlin
 * api.event().events {
 *     queue {
 *         onCreated { println("Queue ${it.queueId()} created") }
 *         onStatusUpdated { println("${it.oldStatus()} -> ${it.newStatus()}") }
 *     }
 * }
 * ```
 *
 * @see EventScope
 */
class QueueEventScope(private val api: QueueEventApi) {

    /** All subscriptions created within this scope. */
    private val _subscriptions = mutableListOf<Subscription>()

    /** The subscriptions created by this scope, for bulk management. */
    val subscriptions: List<Subscription> get() = _subscriptions

    /**
     * Subscribes to [QueueCreatedEvent]s.
     *
     * @param handler called when a new queue is created
     */
    fun onCreated(handler: (QueueCreatedEvent) -> Unit) {
        _subscriptions.add(api.onCreated(handler))
    }

    /**
     * Subscribes to [QueueDeletedEvent]s.
     *
     * @param handler called when a queue is deleted
     */
    fun onDeleted(handler: (QueueDeletedEvent) -> Unit) {
        _subscriptions.add(api.onDeleted(handler))
    }

    /**
     * Subscribes to [QueueStatusUpdatedEvent]s.
     *
     * @param handler called when a queue transitions between statuses
     */
    fun onStatusUpdated(handler: (QueueStatusUpdatedEvent) -> Unit) {
        _subscriptions.add(api.onStatusUpdated(handler))
    }

    /**
     * Subscribes to [QueueUpdatedEvent]s.
     *
     * @param handler called when any queue state changes
     */
    fun onUpdated(handler: (QueueUpdatedEvent) -> Unit) {
        _subscriptions.add(api.onUpdated(handler))
    }

    /**
     * Subscribes to [QueueServerAssignedEvent]s.
     *
     * @param handler called when a server is assigned to a queue
     */
    fun onServerAssigned(handler: (QueueServerAssignedEvent) -> Unit) {
        _subscriptions.add(api.onServerAssigned(handler))
    }

    /**
     * Subscribes to [QueueTransferEvent]s.
     *
     * @param handler called when players are transferred to a game server
     */
    fun onTransfer(handler: (QueueTransferEvent) -> Unit) {
        _subscriptions.add(api.onTransfer(handler))
    }
}

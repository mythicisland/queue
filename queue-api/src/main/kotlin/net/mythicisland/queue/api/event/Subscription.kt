package net.mythicisland.queue.api.event

/**
 * An active event subscription.
 */
interface Subscription : AutoCloseable {

    /**
     * Stops receiving events.
     */
    fun unsubscribe()

    /**
     * Same as [unsubscribe].
     */
    override fun close() = unsubscribe()

}

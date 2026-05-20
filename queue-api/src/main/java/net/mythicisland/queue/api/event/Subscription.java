package net.mythicisland.queue.api.event;

/**
 * Represents an active event subscription.
 */
public interface Subscription extends AutoCloseable {

    /**
     * Cancels the subscription.
     */
    void unsubscribe();

    /**
     * Closes the subscription.
     */
    @Override
    default void close() {
        unsubscribe();
    }
}

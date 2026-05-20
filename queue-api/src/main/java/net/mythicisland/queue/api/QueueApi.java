package net.mythicisland.queue.api;

import net.mythicisland.queue.api.data.QueueDataApi;
import net.mythicisland.queue.api.event.EventApi;
import net.mythicisland.queue.api.internal.QueueApiImpl;
import net.mythicisland.queue.api.player.QueuePlayerApi;

/**
 * Main API entry point.
 */
public interface QueueApi extends AutoCloseable {

    /**
     * Creates a new instance of the API using default options.
     *
     * @return a new QueueApi instance
     */
    static QueueApi create() {
        return create(QueueApiOptions.DEFAULT);
    }

    /**
     * Creates a new instance of the API with the specified options.
     *
     * @param options the configuration options for the API
     * @return a new QueueApi instance
     */
    static QueueApi create(QueueApiOptions options) {
        return new QueueApiImpl(options);
    }

    /**
     * API for performing player-related queuing operations
     *
     * @return the player operation API
     */
    QueuePlayerApi player();

    /**
     * Provides access to data query operations for queues and queue types.
     *
     * @return the data query API
     */
    QueueDataApi data();

    /**
     * Provides access to event subscriptions.
     *
     * @return the event subscription API
     */
    EventApi event();

    /**
     * Closes the API connection.
     */
    @Override
    void close();

}

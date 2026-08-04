package net.mythicisland.queue.api;

import net.mythicisland.queue.api.data.QueueDataApi;
import net.mythicisland.queue.api.event.EventApi;
import net.mythicisland.queue.api.internal.QueueApiImpl;
import net.mythicisland.queue.api.ticket.TicketApi;

/**
 * Main entrypoint to interacting with queue.
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
     * Creates a new instance of the API with specified options.
     *
     * @param options the configuration options for the API
     * @return a new QueueApi instance
     */
    static QueueApi create(QueueApiOptions options) {
        return new QueueApiImpl(options);
    }

    /**
     * Provides ticket related operations.
     *
     * @return the ticket API
     */
    TicketApi ticket();

    /**
     * Provides access to data query operations for tickets, matches and queue types.
     *
     * @return the data query API
     */
    QueueDataApi data();

    /**
     * Provides access to the event API.
     *
     * @return the event API
     */
    EventApi event();

    /**
     * Closes the API connection.
     */
    @Override
    void close();

}

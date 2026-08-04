package net.mythicisland.queue.api.event;

import net.mythicisland.queue.api.event.match.MatchEventApi;
import net.mythicisland.queue.api.event.ticket.TicketEventApi;

/**
 * API for subscribing to events.
 */
public interface EventApi {

    /**
     * Provides access to ticket lifecycle events.
     *
     * @return the ticket event subscription API
     */
    TicketEventApi ticket();

    /**
     * Provides access to match lifecycle events.
     *
     * @return the match event subscription API
     */
    MatchEventApi match();

}

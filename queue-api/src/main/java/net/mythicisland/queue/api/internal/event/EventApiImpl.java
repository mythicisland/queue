package net.mythicisland.queue.api.internal.event;

import io.nats.client.Connection;
import net.mythicisland.queue.api.event.EventApi;
import net.mythicisland.queue.api.event.match.MatchEventApi;
import net.mythicisland.queue.api.event.ticket.TicketEventApi;
import net.mythicisland.queue.api.internal.event.match.MatchEventApiImpl;
import net.mythicisland.queue.api.internal.event.ticket.TicketEventApiImpl;

public final class EventApiImpl implements EventApi {

    private final TicketEventApi ticketEventApi;
    private final MatchEventApi matchEventApi;

    public EventApiImpl(Connection connection) {
        this.ticketEventApi = new TicketEventApiImpl(connection);
        this.matchEventApi = new MatchEventApiImpl(connection);
    }

    @Override
    public TicketEventApi ticket() {
        return ticketEventApi;
    }

    @Override
    public MatchEventApi match() {
        return matchEventApi;
    }
}

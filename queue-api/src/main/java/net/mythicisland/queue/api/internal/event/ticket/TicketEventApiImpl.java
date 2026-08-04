package net.mythicisland.queue.api.internal.event.ticket;

import io.nats.client.Connection;
import net.mythicisland.queue.api.event.Subscription;
import net.mythicisland.queue.api.event.ticket.TicketCreatedEvent;
import net.mythicisland.queue.api.event.ticket.TicketDeletedEvent;
import net.mythicisland.queue.api.event.ticket.TicketEventApi;
import net.mythicisland.queue.api.event.ticket.TicketStateChangedEvent;
import net.mythicisland.queue.api.internal.ProtoUtil;
import net.mythicisland.queue.api.internal.event.NatsEventApi;
import net.mythicisland.queue.api.internal.event.QueueEventSubjects;

import java.util.function.Consumer;

public final class TicketEventApiImpl extends NatsEventApi implements TicketEventApi {

    public TicketEventApiImpl(Connection connection) {
        super(connection);
    }

    @Override
    public Subscription onCreated(Consumer<TicketCreatedEvent> handler) {
        return subscribe(QueueEventSubjects.TICKET_CREATED,
                build.buf.gen.mythicisland.queue.v2.TicketCreatedEvent.parser(),
                proto -> new TicketCreatedEvent(ProtoUtil.toTicket(proto.getTicket())),
                handler);
    }

    @Override
    public Subscription onStateChanged(Consumer<TicketStateChangedEvent> handler) {
        return subscribe(QueueEventSubjects.TICKET_STATE_CHANGED,
                build.buf.gen.mythicisland.queue.v2.TicketStateChangedEvent.parser(),
                proto -> new TicketStateChangedEvent(
                        ProtoUtil.toTicket(proto.getTicket()),
                        ProtoUtil.toTicketState(proto.getPreviousState())
                ),
                handler);
    }

    @Override
    public Subscription onDeleted(Consumer<TicketDeletedEvent> handler) {
        return subscribe(QueueEventSubjects.TICKET_DELETED,
                build.buf.gen.mythicisland.queue.v2.TicketDeletedEvent.parser(),
                proto -> new TicketDeletedEvent(
                        ProtoUtil.toTicket(proto.getTicket()),
                        ProtoUtil.toDeleteReason(proto.getReason())
                ),
                handler);
    }
}

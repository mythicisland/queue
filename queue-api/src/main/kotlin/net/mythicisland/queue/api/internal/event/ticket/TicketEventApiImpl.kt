package net.mythicisland.queue.api.internal.event.ticket

import io.nats.client.Connection
import net.mythicisland.queue.api.event.Subscription
import net.mythicisland.queue.api.event.ticket.TicketCreatedEvent
import net.mythicisland.queue.api.event.ticket.TicketDeletedEvent
import net.mythicisland.queue.api.event.ticket.TicketEventApi
import net.mythicisland.queue.api.event.ticket.TicketStateChangedEvent
import net.mythicisland.queue.api.internal.ProtoUtil
import net.mythicisland.queue.api.internal.event.SubscriptionImpl
import net.mythicisland.queue.shared.event.Subjects
import java.util.function.Consumer
import build.buf.gen.mythicisland.queue.v1.TicketCreatedEvent as ProtoTicketCreatedEvent
import build.buf.gen.mythicisland.queue.v1.TicketDeletedEvent as ProtoTicketDeletedEvent
import build.buf.gen.mythicisland.queue.v1.TicketStateChangedEvent as ProtoTicketStateChangedEvent

internal class TicketEventApiImpl(private val connection: Connection) : TicketEventApi {

    override fun onCreated(handler: Consumer<TicketCreatedEvent>): Subscription {
        val dispatcher = connection.createDispatcher { message ->
            val proto = ProtoTicketCreatedEvent.parseFrom(message.data)
            handler.accept(TicketCreatedEvent(ProtoUtil.toTicket(proto.ticket)))
        }
        dispatcher.subscribe(Subjects.TICKET_CREATED)

        return SubscriptionImpl(connection, dispatcher)
    }

    override fun onStateChanged(handler: Consumer<TicketStateChangedEvent>): Subscription {
        val dispatcher = connection.createDispatcher { message ->
            val proto = ProtoTicketStateChangedEvent.parseFrom(message.data)
            handler.accept(
                TicketStateChangedEvent(
                    ProtoUtil.toTicket(proto.ticket),
                    ProtoUtil.toTicketState(proto.previousState),
                )
            )
        }
        dispatcher.subscribe(Subjects.TICKET_STATE_CHANGED)

        return SubscriptionImpl(connection, dispatcher)
    }

    override fun onDeleted(handler: Consumer<TicketDeletedEvent>): Subscription {
        val dispatcher = connection.createDispatcher { message ->
            val proto = ProtoTicketDeletedEvent.parseFrom(message.data)
            handler.accept(
                TicketDeletedEvent(
                    ProtoUtil.toTicket(proto.ticket),
                    ProtoUtil.toDeleteReason(proto.reason),
                )
            )
        }
        dispatcher.subscribe(Subjects.TICKET_DELETED)

        return SubscriptionImpl(connection, dispatcher)
    }

}

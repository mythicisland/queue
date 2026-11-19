package net.mythicisland.queue.api.internal.event.type

import io.nats.client.Connection
import net.mythicisland.queue.api.event.Subscription
import net.mythicisland.queue.api.event.type.QueueTypeCreatedEvent
import net.mythicisland.queue.api.event.type.QueueTypeDeletedEvent
import net.mythicisland.queue.api.event.type.QueueTypeEventApi
import net.mythicisland.queue.api.event.type.QueueTypeUpdatedEvent
import net.mythicisland.queue.api.internal.ProtoUtil
import net.mythicisland.queue.api.internal.event.SubscriptionImpl
import net.mythicisland.queue.shared.event.Subjects
import java.util.function.Consumer
import build.buf.gen.mythicisland.queue.v1.QueueTypeCreatedEvent as ProtoQueueTypeCreatedEvent
import build.buf.gen.mythicisland.queue.v1.QueueTypeDeletedEvent as ProtoQueueTypeDeletedEvent
import build.buf.gen.mythicisland.queue.v1.QueueTypeUpdatedEvent as ProtoQueueTypeUpdatedEvent

internal class QueueTypeEventApiImpl(private val connection: Connection) : QueueTypeEventApi {

    override fun onCreated(handler: Consumer<QueueTypeCreatedEvent>): Subscription {
        val dispatcher = connection.createDispatcher { message ->
            val proto = ProtoQueueTypeCreatedEvent.parseFrom(message.data)
            handler.accept(QueueTypeCreatedEvent(ProtoUtil.toQueueType(proto.queueType)))
        }
        dispatcher.subscribe(Subjects.QUEUE_TYPE_CREATED)

        return SubscriptionImpl(connection, dispatcher)
    }

    override fun onUpdated(handler: Consumer<QueueTypeUpdatedEvent>): Subscription {
        val dispatcher = connection.createDispatcher { message ->
            val proto = ProtoQueueTypeUpdatedEvent.parseFrom(message.data)
            handler.accept(
                QueueTypeUpdatedEvent(
                    ProtoUtil.toQueueType(proto.queueType),
                    ProtoUtil.toQueueType(proto.previous),
                )
            )
        }
        dispatcher.subscribe(Subjects.QUEUE_TYPE_UPDATED)

        return SubscriptionImpl(connection, dispatcher)
    }

    override fun onDeleted(handler: Consumer<QueueTypeDeletedEvent>): Subscription {
        val dispatcher = connection.createDispatcher { message ->
            val proto = ProtoQueueTypeDeletedEvent.parseFrom(message.data)
            handler.accept(QueueTypeDeletedEvent(ProtoUtil.toQueueType(proto.queueType)))
        }
        dispatcher.subscribe(Subjects.QUEUE_TYPE_DELETED)

        return SubscriptionImpl(connection, dispatcher)
    }

}

package net.mythicisland.queue.api.internal.event.queue;

import com.google.protobuf.InvalidProtocolBufferException;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import net.mythicisland.queue.api.event.Subscription;
import net.mythicisland.queue.api.event.queue.*;
import net.mythicisland.queue.api.internal.ProtoUtil;
import net.mythicisland.queue.api.internal.event.*;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class QueueEventApiImpl implements QueueEventApi {

    private static final Logger LOGGER = Logger.getLogger(QueueEventApiImpl.class.getName());

    private final Connection connection;

    public QueueEventApiImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Subscription onCreated(Consumer<QueueCreatedEvent> handler) {
        Dispatcher dispatcher = connection.createDispatcher(message -> {
            try {
                var proto = build.buf.gen.mythicisland.queue.v1.QueueCreatedEvent.parseFrom(message.getData());
                var queue = proto.getQueue();

                handler.accept(new QueueCreatedEventImpl(
                        ProtoUtil.toQueueId(queue),
                        queue.getType(),
                        ProtoUtil.toApiStatus(queue.getStatus()),
                        ProtoUtil.toUuidList(queue.getPlayerIdsList())
                ));
            } catch (InvalidProtocolBufferException e) {
                LOGGER.log(Level.WARNING, "Failed to deserialize QueueCreatedEvent", e);
            }
        });

        dispatcher.subscribe(QueueEventSubjects.QUEUE_CREATED);
        return new NatsSubscription(dispatcher, QueueEventSubjects.QUEUE_CREATED);
    }

    @Override
    public Subscription onDeleted(Consumer<QueueDeletedEvent> handler) {
        Dispatcher dispatcher = connection.createDispatcher(message -> {
            try {
                var proto = build.buf.gen.mythicisland.queue.v1.QueueDeletedEvent.parseFrom(message.getData());
                var queue = proto.getQueue();

                handler.accept(new QueueDeletedEventImpl(
                        ProtoUtil.toQueueId(queue),
                        queue.getType(),
                        ProtoUtil.toApiStatus(queue.getStatus()),
                        ProtoUtil.toUuidList(queue.getPlayerIdsList())
                ));
            } catch (InvalidProtocolBufferException e) {
                LOGGER.log(Level.WARNING, "Failed to deserialize QueueDeletedEvent", e);
            }
        });

        dispatcher.subscribe(QueueEventSubjects.QUEUE_DELETED);
        return new NatsSubscription(dispatcher, QueueEventSubjects.QUEUE_DELETED);
    }

    @Override
    public Subscription onServerAssigned(Consumer<QueueServerAssignedEvent> handler) {
        Dispatcher dispatcher = connection.createDispatcher(message -> {
            try {
                var proto = build.buf.gen.mythicisland.queue.v1.QueueServerAssignedEvent.parseFrom(message.getData());
                var queue = proto.getQueue();

                handler.accept(new QueueServerAssignedEventImpl(
                        ProtoUtil.toQueueId(queue),
                        queue.getType(),
                        ProtoUtil.toApiStatus(queue.getStatus()),
                        ProtoUtil.toUuidList(queue.getPlayerIdsList()),
                        proto.getServerId()
                ));
            } catch (InvalidProtocolBufferException e) {
                LOGGER.log(Level.WARNING, "Failed to deserialize QueueServerAssignedEvent", e);
            }
        });

        dispatcher.subscribe(QueueEventSubjects.QUEUE_SERVER_ASSIGNED);
        return new NatsSubscription(dispatcher, QueueEventSubjects.QUEUE_SERVER_ASSIGNED);
    }

    @Override
    public Subscription onStatusUpdated(Consumer<QueueStatusUpdatedEvent> handler) {
        Dispatcher dispatcher = connection.createDispatcher(message -> {
            try {
                var proto = build.buf.gen.mythicisland.queue.v1.QueueStatusUpdatedEvent.parseFrom(message.getData());
                var queue = proto.getQueue();

                handler.accept(new QueueStatusUpdatedEventImpl(
                        ProtoUtil.toQueueId(queue),
                        queue.getType(),
                        ProtoUtil.toUuidList(queue.getPlayerIdsList()),
                        ProtoUtil.toApiStatus(proto.getOldStatus()),
                        ProtoUtil.toApiStatus(proto.getNewStatus())
                ));
            } catch (InvalidProtocolBufferException e) {
                LOGGER.log(Level.WARNING, "Failed to deserialize QueueStatusUpdatedEvent", e);
            }
        });

        dispatcher.subscribe(QueueEventSubjects.QUEUE_STATUS_UPDATED);
        return new NatsSubscription(dispatcher, QueueEventSubjects.QUEUE_STATUS_UPDATED);
    }

    @Override
    public Subscription onTransfer(Consumer<QueueTransferEvent> handler) {
        Dispatcher dispatcher = connection.createDispatcher(message -> {
            try {
                var proto = build.buf.gen.mythicisland.queue.v1.QueueTransferEvent.parseFrom(message.getData());
                var queue = proto.getQueue();

                handler.accept(new QueueTransferEventImpl(
                        ProtoUtil.toQueueId(queue),
                        queue.getType(),
                        ProtoUtil.toApiStatus(queue.getStatus()),
                        ProtoUtil.toUuidList(queue.getPlayerIdsList()),
                        proto.getServerId(),
                        ProtoUtil.toUuidList(proto.getPlayerIdsList())
                ));
            } catch (InvalidProtocolBufferException e) {
                LOGGER.log(Level.WARNING, "Failed to deserialize QueueTransferEvent", e);
            }
        });

        dispatcher.subscribe(QueueEventSubjects.QUEUE_TRANSFER);
        return new NatsSubscription(dispatcher, QueueEventSubjects.QUEUE_TRANSFER);
    }

    @Override
    public Subscription onUpdated(Consumer<QueueUpdatedEvent> handler) {
        Dispatcher dispatcher = connection.createDispatcher(message -> {
            try {
                var proto = build.buf.gen.mythicisland.queue.v1.QueueUpdatedEvent.parseFrom(message.getData());
                var before = proto.getBefore();
                var after = proto.getAfter();

                handler.accept(new QueueUpdatedEventImpl(
                        ProtoUtil.toQueueId(after),
                        after.getType(),
                        ProtoUtil.toApiStatus(before.getStatus()),
                        ProtoUtil.toUuidList(before.getPlayerIdsList()),
                        ProtoUtil.toApiStatus(after.getStatus()),
                        ProtoUtil.toUuidList(after.getPlayerIdsList())
                ));
            } catch (InvalidProtocolBufferException e) {
                LOGGER.log(Level.WARNING, "Failed to deserialize QueueUpdatedEvent", e);
            }
        });

        dispatcher.subscribe(QueueEventSubjects.QUEUE_UPDATED);
        return new NatsSubscription(dispatcher, QueueEventSubjects.QUEUE_UPDATED);
    }
}

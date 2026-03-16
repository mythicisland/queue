package net.mythicisland.queue.api.internal.event;

import com.google.protobuf.InvalidProtocolBufferException;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import net.mythicisland.queue.api.event.Subscription;
import net.mythicisland.queue.api.event.queue.*;
import net.mythicisland.queue.api.internal.ProtoConversionUtil;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NATS-backed implementation of {@link QueueEventApi}.
 *
 * <p>Subscribes to NATS subjects, deserializes protobuf messages, converts
 * them to the public API event types, and dispatches to consumer handlers.</p>
 */
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
                        ProtoConversionUtil.toQueueId(queue),
                        queue.getType(),
                        ProtoConversionUtil.toApiStatus(queue.getStatus()),
                        ProtoConversionUtil.toUuidList(queue.getPlayerIdsList())
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
                        ProtoConversionUtil.toQueueId(queue),
                        queue.getType(),
                        ProtoConversionUtil.toApiStatus(queue.getStatus()),
                        ProtoConversionUtil.toUuidList(queue.getPlayerIdsList())
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
                        ProtoConversionUtil.toQueueId(queue),
                        queue.getType(),
                        ProtoConversionUtil.toApiStatus(queue.getStatus()),
                        ProtoConversionUtil.toUuidList(queue.getPlayerIdsList()),
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
                        ProtoConversionUtil.toQueueId(queue),
                        queue.getType(),
                        ProtoConversionUtil.toUuidList(queue.getPlayerIdsList()),
                        ProtoConversionUtil.toApiStatus(proto.getOldStatus()),
                        ProtoConversionUtil.toApiStatus(proto.getNewStatus())
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
                        ProtoConversionUtil.toQueueId(queue),
                        queue.getType(),
                        ProtoConversionUtil.toApiStatus(queue.getStatus()),
                        ProtoConversionUtil.toUuidList(queue.getPlayerIdsList()),
                        proto.getServerId(),
                        ProtoConversionUtil.toUuidList(proto.getPlayerIdsList())
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
                        ProtoConversionUtil.toQueueId(after),
                        after.getType(),
                        ProtoConversionUtil.toApiStatus(before.getStatus()),
                        ProtoConversionUtil.toUuidList(before.getPlayerIdsList()),
                        ProtoConversionUtil.toApiStatus(after.getStatus()),
                        ProtoConversionUtil.toUuidList(after.getPlayerIdsList())
                ));
            } catch (InvalidProtocolBufferException e) {
                LOGGER.log(Level.WARNING, "Failed to deserialize QueueUpdatedEvent", e);
            }
        });

        dispatcher.subscribe(QueueEventSubjects.QUEUE_UPDATED);
        return new NatsSubscription(dispatcher, QueueEventSubjects.QUEUE_UPDATED);
    }
}

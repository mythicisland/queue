package net.mythicisland.queue.api.internal.event;

import com.google.protobuf.InvalidProtocolBufferException;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import net.mythicisland.queue.api.event.Subscription;
import net.mythicisland.queue.api.event.player.DequeueEvent;
import net.mythicisland.queue.api.event.player.EnqueueEvent;
import net.mythicisland.queue.api.event.player.QueuePlayerEventApi;
import net.mythicisland.queue.api.internal.ProtoConversionUtil;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NATS-backed implementation of {@link QueuePlayerEventApi}.
 *
 * <p>Subscribes to NATS subjects, deserializes protobuf messages, converts
 * them to the public API event types, and dispatches to consumer handlers.</p>
 */
public final class QueuePlayerEventApiImpl implements QueuePlayerEventApi {

    private static final Logger LOGGER = Logger.getLogger(QueuePlayerEventApiImpl.class.getName());

    private final Connection connection;

    public QueuePlayerEventApiImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Subscription onEnqueued(Consumer<EnqueueEvent> handler) {
        Dispatcher dispatcher = connection.createDispatcher(message -> {
            try {
                var proto = build.buf.gen.mythicisland.queue.v1.EnqueueEvent.parseFrom(message.getData());
                var queue = proto.getQueue();

                handler.accept(new EnqueueEventImpl(
                        ProtoConversionUtil.toQueueId(queue),
                        queue.getType(),
                        ProtoConversionUtil.toApiStatus(queue.getStatus()),
                        ProtoConversionUtil.toUuidList(queue.getPlayerIdsList()),
                        ProtoConversionUtil.toUuidList(proto.getPlayerIdsList())
                ));
            } catch (InvalidProtocolBufferException e) {
                LOGGER.log(Level.WARNING, "Failed to deserialize EnqueueEvent", e);
            }
        });

        dispatcher.subscribe(QueueEventSubjects.ENQUEUE);
        return new NatsSubscription(dispatcher, QueueEventSubjects.ENQUEUE);
    }

    @Override
    public Subscription onDequeued(Consumer<DequeueEvent> handler) {
        Dispatcher dispatcher = connection.createDispatcher(message -> {
            try {
                var proto = build.buf.gen.mythicisland.queue.v1.DequeueEvent.parseFrom(message.getData());
                var queue = proto.getQueue();

                handler.accept(new DequeueEventImpl(
                        ProtoConversionUtil.toQueueId(queue),
                        queue.getType(),
                        ProtoConversionUtil.toApiStatus(queue.getStatus()),
                        ProtoConversionUtil.toUuidList(queue.getPlayerIdsList()),
                        ProtoConversionUtil.toUuidList(proto.getPlayerIdsList())
                ));
            } catch (InvalidProtocolBufferException e) {
                LOGGER.log(Level.WARNING, "Failed to deserialize DequeueEvent", e);
            }
        });

        dispatcher.subscribe(QueueEventSubjects.DEQUEUE);
        return new NatsSubscription(dispatcher, QueueEventSubjects.DEQUEUE);
    }
}

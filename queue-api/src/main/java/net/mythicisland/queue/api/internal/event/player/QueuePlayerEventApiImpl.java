package net.mythicisland.queue.api.internal.event.player;

import com.google.protobuf.InvalidProtocolBufferException;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import net.mythicisland.queue.api.event.Subscription;
import net.mythicisland.queue.api.event.player.DequeueEvent;
import net.mythicisland.queue.api.event.player.EnqueueEvent;
import net.mythicisland.queue.api.event.player.QueuePlayerEventApi;
import net.mythicisland.queue.api.internal.ProtoUtil;
import net.mythicisland.queue.api.internal.event.NatsSubscription;
import net.mythicisland.queue.api.internal.event.QueueEventSubjects;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

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
                        ProtoUtil.toQueueId(queue),
                        queue.getType(),
                        ProtoUtil.toApiStatus(queue.getStatus()),
                        ProtoUtil.toUuidList(queue.getPlayerIdsList()),
                        ProtoUtil.toUuidList(proto.getPlayerIdsList())
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
                        ProtoUtil.toQueueId(queue),
                        queue.getType(),
                        ProtoUtil.toApiStatus(queue.getStatus()),
                        ProtoUtil.toUuidList(queue.getPlayerIdsList()),
                        ProtoUtil.toUuidList(proto.getPlayerIdsList())
                ));
            } catch (InvalidProtocolBufferException e) {
                LOGGER.log(Level.WARNING, "Failed to deserialize DequeueEvent", e);
            }
        });

        dispatcher.subscribe(QueueEventSubjects.DEQUEUE);
        return new NatsSubscription(dispatcher, QueueEventSubjects.DEQUEUE);
    }
}

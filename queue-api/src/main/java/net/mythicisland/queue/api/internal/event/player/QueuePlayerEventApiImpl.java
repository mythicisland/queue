package net.mythicisland.queue.api.internal.event.player;

import io.nats.client.Connection;
import net.mythicisland.queue.api.event.Subscription;
import net.mythicisland.queue.api.event.player.DequeueEvent;
import net.mythicisland.queue.api.event.player.EnqueueEvent;
import net.mythicisland.queue.api.event.player.QueuePlayerEventApi;
import net.mythicisland.queue.api.internal.ProtoUtil;
import net.mythicisland.queue.api.internal.event.NatsEventApi;
import net.mythicisland.queue.api.internal.event.QueueEventSubjects;

import java.util.function.Consumer;

public final class QueuePlayerEventApiImpl extends NatsEventApi implements QueuePlayerEventApi {

    public QueuePlayerEventApiImpl(Connection connection) {
        super(connection);
    }

    @Override
    public Subscription onEnqueued(Consumer<EnqueueEvent> handler) {
        return subscribe(QueueEventSubjects.ENQUEUE,
                build.buf.gen.mythicisland.queue.v1.EnqueueEvent.parser(),
                proto -> ProtoUtil.fromQueue(proto.getQueue(), (id, type, status, queuePlayerIds) ->
                        new EnqueueEvent(id, type, status, queuePlayerIds,
                                ProtoUtil.toUuidList(proto.getPlayerIdsList()))),
                handler);
    }

    @Override
    public Subscription onDequeued(Consumer<DequeueEvent> handler) {
        return subscribe(QueueEventSubjects.DEQUEUE,
                build.buf.gen.mythicisland.queue.v1.DequeueEvent.parser(),
                proto -> ProtoUtil.fromQueue(proto.getQueue(), (id, type, status, queuePlayerIds) ->
                        new DequeueEvent(id, type, status, queuePlayerIds,
                                ProtoUtil.toUuidList(proto.getPlayerIdsList()))),
                handler);
    }
}

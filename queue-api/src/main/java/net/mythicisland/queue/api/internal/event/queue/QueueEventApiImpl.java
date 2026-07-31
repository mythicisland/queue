package net.mythicisland.queue.api.internal.event.queue;

import io.nats.client.Connection;
import net.mythicisland.queue.api.event.Subscription;
import net.mythicisland.queue.api.event.queue.*;
import net.mythicisland.queue.api.internal.ProtoUtil;
import net.mythicisland.queue.api.internal.event.NatsEventApi;
import net.mythicisland.queue.api.internal.event.QueueEventSubjects;

import java.util.function.Consumer;

public final class QueueEventApiImpl extends NatsEventApi implements QueueEventApi {

    public QueueEventApiImpl(Connection connection) {
        super(connection);
    }

    @Override
    public Subscription onCreated(Consumer<QueueCreatedEvent> handler) {
        return subscribe(QueueEventSubjects.QUEUE_CREATED,
                build.buf.gen.mythicisland.queue.v1.QueueCreatedEvent.parser(),
                proto -> ProtoUtil.fromQueue(proto.getQueue(), QueueCreatedEvent::new),
                handler);
    }

    @Override
    public Subscription onDeleted(Consumer<QueueDeletedEvent> handler) {
        return subscribe(QueueEventSubjects.QUEUE_DELETED,
                build.buf.gen.mythicisland.queue.v1.QueueDeletedEvent.parser(),
                proto -> ProtoUtil.fromQueue(proto.getQueue(), QueueDeletedEvent::new),
                handler);
    }

    @Override
    public Subscription onServerAssigned(Consumer<QueueServerAssignedEvent> handler) {
        return subscribe(QueueEventSubjects.QUEUE_SERVER_ASSIGNED,
                build.buf.gen.mythicisland.queue.v1.QueueServerAssignedEvent.parser(),
                proto -> ProtoUtil.fromQueue(proto.getQueue(), (id, type, status, playerIds) ->
                        new QueueServerAssignedEvent(id, type, status, playerIds, proto.getServerId())),
                handler);
    }

    @Override
    public Subscription onStatusUpdated(Consumer<QueueStatusUpdatedEvent> handler) {
        return subscribe(QueueEventSubjects.QUEUE_STATUS_UPDATED,
                build.buf.gen.mythicisland.queue.v1.QueueStatusUpdatedEvent.parser(),
                proto -> new QueueStatusUpdatedEvent(
                        ProtoUtil.toQueueId(proto.getQueue()),
                        proto.getQueue().getType(),
                        ProtoUtil.toUuidList(proto.getQueue().getPlayerIdsList()),
                        ProtoUtil.toApiStatus(proto.getOldStatus()),
                        ProtoUtil.toApiStatus(proto.getNewStatus())
                ),
                handler);
    }

    @Override
    public Subscription onTransfer(Consumer<QueueTransferEvent> handler) {
        return subscribe(QueueEventSubjects.QUEUE_TRANSFER,
                build.buf.gen.mythicisland.queue.v1.QueueTransferEvent.parser(),
                proto -> ProtoUtil.fromQueue(proto.getQueue(), (id, type, status, playerIds) ->
                        new QueueTransferEvent(id, type, status, playerIds, proto.getServerId(),
                                ProtoUtil.toUuidList(proto.getPlayerIdsList()))),
                handler);
    }

    @Override
    public Subscription onUpdated(Consumer<QueueUpdatedEvent> handler) {
        return subscribe(QueueEventSubjects.QUEUE_UPDATED,
                build.buf.gen.mythicisland.queue.v1.QueueUpdatedEvent.parser(),
                proto -> new QueueUpdatedEvent(
                        ProtoUtil.toQueueId(proto.getAfter()),
                        proto.getAfter().getType(),
                        ProtoUtil.toApiStatus(proto.getBefore().getStatus()),
                        ProtoUtil.toUuidList(proto.getBefore().getPlayerIdsList()),
                        ProtoUtil.toApiStatus(proto.getAfter().getStatus()),
                        ProtoUtil.toUuidList(proto.getAfter().getPlayerIdsList())
                ),
                handler);
    }
}

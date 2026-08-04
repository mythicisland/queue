package net.mythicisland.queue.api.internal.event.match;

import io.nats.client.Connection;
import net.mythicisland.queue.api.event.Subscription;
import net.mythicisland.queue.api.event.match.MatchCreatedEvent;
import net.mythicisland.queue.api.event.match.MatchEventApi;
import net.mythicisland.queue.api.event.match.MatchStateChangedEvent;
import net.mythicisland.queue.api.event.match.MatchTransferredEvent;
import net.mythicisland.queue.api.internal.ProtoUtil;
import net.mythicisland.queue.api.internal.event.NatsEventApi;
import net.mythicisland.queue.api.internal.event.QueueEventSubjects;

import java.util.function.Consumer;

public final class MatchEventApiImpl extends NatsEventApi implements MatchEventApi {

    public MatchEventApiImpl(Connection connection) {
        super(connection);
    }

    @Override
    public Subscription onCreated(Consumer<MatchCreatedEvent> handler) {
        return subscribe(QueueEventSubjects.MATCH_CREATED,
                build.buf.gen.mythicisland.queue.v2.MatchCreatedEvent.parser(),
                proto -> new MatchCreatedEvent(ProtoUtil.toMatch(proto.getMatch())),
                handler);
    }

    @Override
    public Subscription onStateChanged(Consumer<MatchStateChangedEvent> handler) {
        return subscribe(QueueEventSubjects.MATCH_STATE_CHANGED,
                build.buf.gen.mythicisland.queue.v2.MatchStateChangedEvent.parser(),
                proto -> new MatchStateChangedEvent(
                        ProtoUtil.toMatch(proto.getMatch()),
                        ProtoUtil.toMatchState(proto.getPreviousState())
                ),
                handler);
    }

    @Override
    public Subscription onTransferred(Consumer<MatchTransferredEvent> handler) {
        return subscribe(QueueEventSubjects.MATCH_TRANSFERRED,
                build.buf.gen.mythicisland.queue.v2.MatchTransferredEvent.parser(),
                proto -> new MatchTransferredEvent(
                        ProtoUtil.toMatch(proto.getMatch()),
                        ProtoUtil.toUuidList(proto.getTransferredPlayerIdsList())
                ),
                handler);
    }
}

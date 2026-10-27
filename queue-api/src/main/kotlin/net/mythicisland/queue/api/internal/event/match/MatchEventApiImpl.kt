package net.mythicisland.queue.api.internal.event.match

import io.nats.client.Connection
import net.mythicisland.queue.api.event.Subscription
import net.mythicisland.queue.api.event.match.MatchCreatedEvent
import net.mythicisland.queue.api.event.match.MatchEventApi
import net.mythicisland.queue.api.event.match.MatchReleasedEvent
import net.mythicisland.queue.api.event.match.MatchStateChangedEvent
import net.mythicisland.queue.api.event.match.MatchTransferredEvent
import net.mythicisland.queue.api.internal.ProtoUtil
import net.mythicisland.queue.api.internal.event.SubscriptionImpl
import net.mythicisland.queue.shared.event.Subjects
import java.util.function.Consumer
import build.buf.gen.mythicisland.queue.v1.MatchCreatedEvent as ProtoMatchCreatedEvent
import build.buf.gen.mythicisland.queue.v1.MatchReleasedEvent as ProtoMatchReleasedEvent
import build.buf.gen.mythicisland.queue.v1.MatchStateChangedEvent as ProtoMatchStateChangedEvent
import build.buf.gen.mythicisland.queue.v1.MatchTransferredEvent as ProtoMatchTransferredEvent

internal class MatchEventApiImpl(
    private val connection: Connection,
    private val serverId: String?,
) : MatchEventApi {

    override fun onCreated(handler: Consumer<MatchCreatedEvent>): Subscription {
        val dispatcher = connection.createDispatcher { message ->
            val proto = ProtoMatchCreatedEvent.parseFrom(message.data)
            handler.accept(MatchCreatedEvent(ProtoUtil.toMatch(proto.match)))
        }
        dispatcher.subscribe(subject(Subjects.MATCH_CREATED))

        return SubscriptionImpl(connection, dispatcher)
    }

    override fun onStateChanged(handler: Consumer<MatchStateChangedEvent>): Subscription {
        val dispatcher = connection.createDispatcher { message ->
            val proto = ProtoMatchStateChangedEvent.parseFrom(message.data)
            handler.accept(
                MatchStateChangedEvent(
                    ProtoUtil.toMatch(proto.match),
                    ProtoUtil.toMatchState(proto.previousState),
                )
            )
        }
        dispatcher.subscribe(subject(Subjects.MATCH_STATE_CHANGED))

        return SubscriptionImpl(connection, dispatcher)
    }

    override fun onTransferred(handler: Consumer<MatchTransferredEvent>): Subscription {
        val dispatcher = connection.createDispatcher { message ->
            val proto = ProtoMatchTransferredEvent.parseFrom(message.data)
            handler.accept(
                MatchTransferredEvent(
                    ProtoUtil.toMatch(proto.match),
                    ProtoUtil.toUuids(proto.transferredPlayerIdsList),
                    ProtoUtil.toUuids(proto.missingPlayerIdsList),
                )
            )
        }
        dispatcher.subscribe(subject(Subjects.MATCH_TRANSFERRED))

        return SubscriptionImpl(connection, dispatcher)
    }

    override fun onReleased(handler: Consumer<MatchReleasedEvent>): Subscription {
        val dispatcher = connection.createDispatcher { message ->
            val proto = ProtoMatchReleasedEvent.parseFrom(message.data)
            handler.accept(
                MatchReleasedEvent(
                    ProtoUtil.toMatch(proto.match),
                    ProtoUtil.toReleaseReason(proto.reason),
                )
            )
        }
        dispatcher.subscribe(subject(Subjects.MATCH_RELEASED))

        return SubscriptionImpl(connection, dispatcher)
    }

    private fun subject(subject: String): String = if (serverId == null) subject else Subjects.server(serverId, subject)

}

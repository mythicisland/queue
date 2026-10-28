package net.mythicisland.queue.api.internal.match

import build.buf.gen.mythicisland.queue.v1.AcceptMatchRequest
import build.buf.gen.mythicisland.queue.v1.GameServerServiceGrpcKt.GameServerServiceCoroutineStub
import build.buf.gen.mythicisland.queue.v1.RejectMatchRequest
import build.buf.gen.mythicisland.queue.v1.ReleaseMatchRequest
import io.grpc.StatusException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import net.mythicisland.queue.api.QueueException
import net.mythicisland.queue.api.event.EventApi
import net.mythicisland.queue.api.event.Subscription
import net.mythicisland.queue.api.internal.ProtoUtil
import net.mythicisland.queue.api.match.Match
import net.mythicisland.queue.api.match.MatchApi
import net.mythicisland.queue.api.match.MatchState
import net.mythicisland.queue.api.match.OfferDecision
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.function.Function

internal class MatchApiImpl(
    private val stub: GameServerServiceCoroutineStub,
    private val scope: CoroutineScope,
    private val events: EventApi,
    private val defaultServerId: String?,
) : MatchApi {

    override suspend fun acceptMatch(serverId: String, matchId: UUID): Match {
        val request = AcceptMatchRequest.newBuilder()
            .setServerId(serverId)
            .setMatchId(matchId.toString())
            .build()

        try {
            return ProtoUtil.toMatch(stub.acceptMatch(request).match)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override suspend fun rejectMatch(serverId: String, matchId: UUID, reason: String) {
        val request = RejectMatchRequest.newBuilder()
            .setServerId(serverId)
            .setMatchId(matchId.toString())
            .setReason(reason)
            .build()

        try {
            stub.rejectMatch(request)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override suspend fun releaseMatch(serverId: String, matchId: UUID) {
        val request = ReleaseMatchRequest.newBuilder()
            .setServerId(serverId)
            .setMatchId(matchId.toString())
            .build()

        try {
            stub.releaseMatch(request)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override suspend fun acceptMatch(matchId: UUID): Match = acceptMatch(currentServerId(), matchId)

    override suspend fun rejectMatch(matchId: UUID, reason: String) = rejectMatch(currentServerId(), matchId, reason)

    override suspend fun releaseMatch(matchId: UUID) = releaseMatch(currentServerId(), matchId)

    override fun acceptMatchAsync(serverId: String, matchId: UUID): CompletableFuture<Match> =
        scope.future { acceptMatch(serverId, matchId) }

    override fun acceptMatchAsync(matchId: UUID): CompletableFuture<Match> = acceptMatchAsync(currentServerId(), matchId)

    override fun rejectMatchAsync(serverId: String, matchId: UUID, reason: String): CompletableFuture<Void?> =
        scope.future {
            rejectMatch(serverId, matchId, reason)
            null
        }

    override fun rejectMatchAsync(matchId: UUID, reason: String): CompletableFuture<Void?> =
        rejectMatchAsync(currentServerId(), matchId, reason)

    override fun releaseMatchAsync(serverId: String, matchId: UUID): CompletableFuture<Void?> = scope.future {
        releaseMatch(serverId, matchId)
        null
    }

    override fun releaseMatchAsync(matchId: UUID): CompletableFuture<Void?> = releaseMatchAsync(currentServerId(), matchId)

    override fun onOffered(serverId: String, handler: Function<Match, OfferDecision>): Subscription =
        events.match(serverId).onStateChanged { event ->
            val match = event.match
            if (match.state == MatchState.OFFERED && match.assignment?.serverId == serverId) {
                scope.launch { decide(serverId, match, handler) }
            }
        }

    override fun onOffered(handler: Function<Match, OfferDecision>): Subscription = onOffered(currentServerId(), handler)

    private fun currentServerId(): String =
        defaultServerId ?: throw IllegalStateException("No server id set, pass one or set SIMPLECLOUD_UNIQUE_ID")

    private suspend fun decide(serverId: String, match: Match, handler: Function<Match, OfferDecision>) {
        val decision = try {
            handler.apply(match)
        } catch (e: Exception) {
            OfferDecision.reject("Offer handler failed: ${e.message}")
        }

        try {
            if (decision.accepted) {
                acceptMatch(serverId, match.id)
            } else {
                rejectMatch(serverId, match.id, decision.reason)
            }
        } catch (_: QueueException) {
        }
    }

}

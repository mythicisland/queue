package net.mythicisland.queue.api.internal.match

import build.buf.gen.mythicisland.queue.v1.AcceptMatchRequest
import build.buf.gen.mythicisland.queue.v1.GameServerServiceGrpcKt.GameServerServiceCoroutineStub
import build.buf.gen.mythicisland.queue.v1.RejectMatchRequest
import build.buf.gen.mythicisland.queue.v1.ReleaseMatchRequest
import io.grpc.StatusException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import net.mythicisland.queue.api.internal.ProtoUtil
import net.mythicisland.queue.api.match.Match
import net.mythicisland.queue.api.match.MatchApi
import java.util.UUID
import java.util.concurrent.CompletableFuture

internal class MatchApiImpl(
    private val stub: GameServerServiceCoroutineStub,
    private val scope: CoroutineScope,
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

    override fun acceptMatchAsync(serverId: String, matchId: UUID): CompletableFuture<Match> =
        scope.future { acceptMatch(serverId, matchId) }

    override fun rejectMatchAsync(serverId: String, matchId: UUID, reason: String): CompletableFuture<Void?> =
        scope.future {
            rejectMatch(serverId, matchId, reason)
            null
        }

    override fun releaseMatchAsync(serverId: String, matchId: UUID): CompletableFuture<Void?> = scope.future {
        releaseMatch(serverId, matchId)
        null
    }

}

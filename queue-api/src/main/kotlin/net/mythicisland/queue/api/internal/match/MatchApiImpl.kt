package net.mythicisland.queue.api.internal.match

import build.buf.gen.mythicisland.queue.v1.AcceptMatchRequest
import build.buf.gen.mythicisland.queue.v1.GameServerServiceGrpcKt.GameServerServiceCoroutineStub
import build.buf.gen.mythicisland.queue.v1.RejectMatchRequest
import build.buf.gen.mythicisland.queue.v1.ReleaseMatchRequest
import net.mythicisland.queue.api.internal.ProtoUtil
import net.mythicisland.queue.api.match.Match
import net.mythicisland.queue.api.match.MatchApi
import java.util.UUID

internal class MatchApiImpl(private val stub: GameServerServiceCoroutineStub) : MatchApi {

    override suspend fun acceptMatch(serverId: String, matchId: UUID): Match {
        val request = AcceptMatchRequest.newBuilder()
            .setServerId(serverId)
            .setMatchId(matchId.toString())
            .build()

        return ProtoUtil.toMatch(stub.acceptMatch(request).match)
    }

    override suspend fun rejectMatch(serverId: String, matchId: UUID, reason: String) {
        val request = RejectMatchRequest.newBuilder()
            .setServerId(serverId)
            .setMatchId(matchId.toString())
            .setReason(reason)
            .build()

        stub.rejectMatch(request)
    }

    override suspend fun releaseMatch(serverId: String, matchId: UUID) {
        val request = ReleaseMatchRequest.newBuilder()
            .setServerId(serverId)
            .setMatchId(matchId.toString())
            .build()

        stub.releaseMatch(request)
    }

}

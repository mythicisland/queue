package net.mythicisland.queue.runtime.server

import build.buf.gen.mythicisland.queue.v1.AcceptMatchRequest
import build.buf.gen.mythicisland.queue.v1.AcceptMatchResponse
import build.buf.gen.mythicisland.queue.v1.GameServerServiceGrpcKt
import build.buf.gen.mythicisland.queue.v1.MatchReleaseReason
import build.buf.gen.mythicisland.queue.v1.RejectMatchRequest
import build.buf.gen.mythicisland.queue.v1.RejectMatchResponse
import build.buf.gen.mythicisland.queue.v1.ReleaseMatchRequest
import build.buf.gen.mythicisland.queue.v1.ReleaseMatchResponse
import build.buf.gen.mythicisland.queue.v1.acceptMatchResponse
import io.grpc.Status
import net.mythicisland.queue.runtime.match.MatchReconciler
import net.mythicisland.queue.runtime.match.MatchStore
import net.mythicisland.queue.shared.match.Match
import net.mythicisland.queue.shared.match.MatchState

class GameServerService(
    private val store: MatchStore,
    private val allocator: ServerAllocator,
    private val reconciler: MatchReconciler,
) : GameServerServiceGrpcKt.GameServerServiceCoroutineImplBase() {

    override suspend fun acceptMatch(request: AcceptMatchRequest): AcceptMatchResponse {
        val match = offered(request.serverId, request.matchId)
        val accepted = reconciler.accept(match, request.serverId) ?: throw notOffered(match)
        return acceptMatchResponse { this.match = accepted.toProto() }
    }

    override suspend fun rejectMatch(request: RejectMatchRequest): RejectMatchResponse {
        val match = offered(request.serverId, request.matchId)
        if (!reconciler.reject(match, request.serverId, request.reason)) {
            throw notOffered(match)
        }
        return RejectMatchResponse.getDefaultInstance()
    }

    override suspend fun releaseMatch(request: ReleaseMatchRequest): ReleaseMatchResponse {
        val match = store.get(request.matchId)
        if (match == null) {
            if (allocator.release(request.serverId, request.matchId)) {
                return ReleaseMatchResponse.getDefaultInstance()
            }
            throw notFound(request.matchId)
        }
        if (match.assignment?.serverId != request.serverId) {
            throw Status.PERMISSION_DENIED.withDescription("Match ${match.id} is not hosted by this server").asException()
        }
        if (match.state != MatchState.COMPLETED) {
            throw Status.FAILED_PRECONDITION.withDescription("Match ${match.id} is ${match.state}, not COMPLETED").asException()
        }

        reconciler.release(match, MatchReleaseReason.MATCH_RELEASE_REASON_RELEASED)
        return ReleaseMatchResponse.getDefaultInstance()
    }

    private fun offered(serverId: String, matchId: String): Match {
        val match = store.get(matchId) ?: throw notFound(matchId)
        if (match.state == MatchState.OFFERED && match.assignment?.serverId == serverId) {
            return match
        }
        if (match.assignment?.serverId == serverId || reconciler.isRejected(matchId, serverId)) {
            throw notOffered(match)
        }
        throw Status.PERMISSION_DENIED.withDescription("Match $matchId is not offered to this server").asException()
    }

    private fun notFound(matchId: String) = Status.NOT_FOUND.withDescription("Match $matchId does not exist").asException()

    private fun notOffered(match: Match) =
        Status.FAILED_PRECONDITION.withDescription("Match ${match.id} is not offered anymore").asException()

}

package net.mythicisland.queue.api.internal.data

import build.buf.gen.mythicisland.queue.v1.GetMatchRequest
import build.buf.gen.mythicisland.queue.v1.GetQueueStatsRequest
import build.buf.gen.mythicisland.queue.v1.GetQueueTypeRequest
import build.buf.gen.mythicisland.queue.v1.GetTicketByPlayerRequest
import build.buf.gen.mythicisland.queue.v1.GetTicketRequest
import build.buf.gen.mythicisland.queue.v1.ListMatchesRequest
import build.buf.gen.mythicisland.queue.v1.ListQueueStatsRequest
import build.buf.gen.mythicisland.queue.v1.ListQueueTypesRequest
import build.buf.gen.mythicisland.queue.v1.ListTicketsRequest
import build.buf.gen.mythicisland.queue.v1.QueueDataServiceGrpcKt.QueueDataServiceCoroutineStub
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import net.mythicisland.queue.api.data.QueueDataApi
import net.mythicisland.queue.api.internal.ProtoUtil
import net.mythicisland.queue.api.match.Match
import net.mythicisland.queue.api.stats.QueueStats
import net.mythicisland.queue.api.ticket.Ticket
import net.mythicisland.queue.api.type.QueueType
import java.util.UUID
import java.util.concurrent.CompletableFuture

internal class QueueDataApiImpl(
    private val stub: QueueDataServiceCoroutineStub,
    private val scope: CoroutineScope,
) : QueueDataApi {

    override suspend fun getTicket(ticketId: UUID): Ticket? {
        val request = GetTicketRequest.newBuilder()
            .setTicketId(ticketId.toString())
            .build()

        try {
            return ProtoUtil.toTicket(stub.getTicket(request).ticket)
        } catch (e: StatusException) {
            if (e.status.code != Status.Code.NOT_FOUND) throw ProtoUtil.toQueueException(e)
            return null
        }
    }

    override suspend fun getTicketByPlayer(playerId: UUID): Ticket? {
        val request = GetTicketByPlayerRequest.newBuilder()
            .setPlayerId(playerId.toString())
            .build()

        try {
            return ProtoUtil.toTicket(stub.getTicketByPlayer(request).ticket)
        } catch (e: StatusException) {
            if (e.status.code != Status.Code.NOT_FOUND) throw ProtoUtil.toQueueException(e)
            return null
        }
    }

    override suspend fun listTickets(queueType: String?): List<Ticket> {
        val request = ListTicketsRequest.newBuilder()
        if (queueType != null) {
            request.setQueueType(queueType)
        }

        try {
            return stub.listTickets(request.build()).ticketsList.map(ProtoUtil::toTicket)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override suspend fun getMatch(matchId: UUID): Match? {
        val request = GetMatchRequest.newBuilder()
            .setMatchId(matchId.toString())
            .build()

        try {
            return ProtoUtil.toMatch(stub.getMatch(request).match)
        } catch (e: StatusException) {
            if (e.status.code != Status.Code.NOT_FOUND) throw ProtoUtil.toQueueException(e)
            return null
        }
    }

    override suspend fun listMatches(queueType: String?, serverId: String?): List<Match> {
        val request = ListMatchesRequest.newBuilder()
        if (queueType != null) {
            request.setQueueType(queueType)
        }
        if (serverId != null) {
            request.setServerId(serverId)
        }

        try {
            return stub.listMatches(request.build()).matchesList.map(ProtoUtil::toMatch)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override suspend fun getQueueType(name: String): QueueType? {
        val request = GetQueueTypeRequest.newBuilder()
            .setName(name)
            .build()

        try {
            return ProtoUtil.toQueueType(stub.getQueueType(request).queueType)
        } catch (e: StatusException) {
            if (e.status.code != Status.Code.NOT_FOUND) throw ProtoUtil.toQueueException(e)
            return null
        }
    }

    override suspend fun listQueueTypes(): List<QueueType> {
        try {
            return stub.listQueueTypes(ListQueueTypesRequest.getDefaultInstance()).queueTypesList.map(ProtoUtil::toQueueType)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override suspend fun getQueueStats(queueType: String): QueueStats? {
        val request = GetQueueStatsRequest.newBuilder()
            .setQueueType(queueType)
            .build()

        try {
            return ProtoUtil.toQueueStats(stub.getQueueStats(request).stats)
        } catch (e: StatusException) {
            if (e.status.code != Status.Code.NOT_FOUND) throw ProtoUtil.toQueueException(e)
            return null
        }
    }

    override suspend fun listQueueStats(): List<QueueStats> {
        try {
            return stub.listQueueStats(ListQueueStatsRequest.getDefaultInstance()).statsList.map(ProtoUtil::toQueueStats)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override fun getTicketAsync(ticketId: UUID): CompletableFuture<Ticket?> = scope.future { getTicket(ticketId) }

    override fun getTicketByPlayerAsync(playerId: UUID): CompletableFuture<Ticket?> =
        scope.future { getTicketByPlayer(playerId) }

    override fun listTicketsAsync(queueType: String?): CompletableFuture<List<Ticket>> =
        scope.future { listTickets(queueType) }

    override fun getMatchAsync(matchId: UUID): CompletableFuture<Match?> = scope.future { getMatch(matchId) }

    override fun listMatchesAsync(queueType: String?, serverId: String?): CompletableFuture<List<Match>> =
        scope.future { listMatches(queueType, serverId) }

    override fun getQueueTypeAsync(name: String): CompletableFuture<QueueType?> = scope.future { getQueueType(name) }

    override fun listQueueTypesAsync(): CompletableFuture<List<QueueType>> = scope.future { listQueueTypes() }

    override fun getQueueStatsAsync(queueType: String): CompletableFuture<QueueStats?> =
        scope.future { getQueueStats(queueType) }

    override fun listQueueStatsAsync(): CompletableFuture<List<QueueStats>> = scope.future { listQueueStats() }

}

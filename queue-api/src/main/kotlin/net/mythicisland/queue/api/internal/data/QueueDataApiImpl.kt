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
import net.mythicisland.queue.api.data.QueueDataApi
import net.mythicisland.queue.api.internal.ProtoUtil
import net.mythicisland.queue.api.match.Match
import net.mythicisland.queue.api.stats.QueueStats
import net.mythicisland.queue.api.ticket.Ticket
import net.mythicisland.queue.api.type.QueueType
import java.util.UUID

internal class QueueDataApiImpl(private val stub: QueueDataServiceCoroutineStub) : QueueDataApi {

    override suspend fun getTicket(ticketId: UUID): Ticket? {
        val request = GetTicketRequest.newBuilder()
            .setTicketId(ticketId.toString())
            .build()

        try {
            return ProtoUtil.toTicket(stub.getTicket(request).ticket)
        } catch (e: StatusException) {
            if (e.status.code != Status.Code.NOT_FOUND) throw e
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
            if (e.status.code != Status.Code.NOT_FOUND) throw e
            return null
        }
    }

    override suspend fun listTickets(queueType: String?): List<Ticket> {
        val request = ListTicketsRequest.newBuilder()
        if (queueType != null) {
            request.setQueueType(queueType)
        }

        return stub.listTickets(request.build()).ticketsList.map(ProtoUtil::toTicket)
    }

    override suspend fun getMatch(matchId: UUID): Match? {
        val request = GetMatchRequest.newBuilder()
            .setMatchId(matchId.toString())
            .build()

        try {
            return ProtoUtil.toMatch(stub.getMatch(request).match)
        } catch (e: StatusException) {
            if (e.status.code != Status.Code.NOT_FOUND) throw e
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

        return stub.listMatches(request.build()).matchesList.map(ProtoUtil::toMatch)
    }

    override suspend fun getQueueType(name: String): QueueType? {
        val request = GetQueueTypeRequest.newBuilder()
            .setName(name)
            .build()

        try {
            return ProtoUtil.toQueueType(stub.getQueueType(request).queueType)
        } catch (e: StatusException) {
            if (e.status.code != Status.Code.NOT_FOUND) throw e
            return null
        }
    }

    override suspend fun listQueueTypes(): List<QueueType> {
        return stub.listQueueTypes(ListQueueTypesRequest.getDefaultInstance()).queueTypesList.map(ProtoUtil::toQueueType)
    }

    override suspend fun getQueueStats(queueType: String): QueueStats? {
        val request = GetQueueStatsRequest.newBuilder()
            .setQueueType(queueType)
            .build()

        try {
            return ProtoUtil.toQueueStats(stub.getQueueStats(request).stats)
        } catch (e: StatusException) {
            if (e.status.code != Status.Code.NOT_FOUND) throw e
            return null
        }
    }

    override suspend fun listQueueStats(): List<QueueStats> {
        return stub.listQueueStats(ListQueueStatsRequest.getDefaultInstance()).statsList.map(ProtoUtil::toQueueStats)
    }

}

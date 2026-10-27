package net.mythicisland.queue.api.internal.ticket

import build.buf.gen.mythicisland.queue.v1.CreateTicketRequest
import build.buf.gen.mythicisland.queue.v1.DeleteTicketRequest
import build.buf.gen.mythicisland.queue.v1.TicketServiceGrpcKt.TicketServiceCoroutineStub
import net.mythicisland.queue.api.internal.ProtoUtil
import net.mythicisland.queue.api.ticket.Ticket
import net.mythicisland.queue.api.ticket.TicketApi
import java.util.UUID

internal class TicketApiImpl(private val stub: TicketServiceCoroutineStub) : TicketApi {

    override suspend fun createTicket(
        playerIds: List<UUID>,
        queueTypes: List<String>,
        properties: Map<String, String>,
    ): Ticket {
        val request = CreateTicketRequest.newBuilder()
            .addAllPlayerIds(playerIds.map(UUID::toString))
            .addAllQueueTypes(queueTypes)
            .putAllProperties(properties)
            .build()

        return ProtoUtil.toTicket(stub.createTicket(request).ticket)
    }

    override suspend fun deleteTicket(ticketId: UUID) {
        val request = DeleteTicketRequest.newBuilder()
            .setTicketId(ticketId.toString())
            .build()

        stub.deleteTicket(request)
    }

    override suspend fun deleteTicketByPlayer(playerId: UUID) {
        val request = DeleteTicketRequest.newBuilder()
            .setPlayerId(playerId.toString())
            .build()

        stub.deleteTicket(request)
    }

}

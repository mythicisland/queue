package net.mythicisland.queue.api.internal.ticket

import build.buf.gen.mythicisland.queue.v1.CreateTicketRequest
import build.buf.gen.mythicisland.queue.v1.DeleteTicketRequest
import build.buf.gen.mythicisland.queue.v1.TicketServiceGrpcKt.TicketServiceCoroutineStub
import build.buf.gen.mythicisland.queue.v1.UpdateTicketPropertiesRequest
import io.grpc.StatusException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import net.mythicisland.queue.api.internal.ProtoUtil
import net.mythicisland.queue.api.ticket.Ticket
import net.mythicisland.queue.api.ticket.TicketApi
import java.util.UUID
import java.util.concurrent.CompletableFuture

internal class TicketApiImpl(
    private val stub: TicketServiceCoroutineStub,
    private val scope: CoroutineScope,
) : TicketApi {

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

        try {
            return ProtoUtil.toTicket(stub.createTicket(request).ticket)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override suspend fun deleteTicket(ticketId: UUID) {
        val request = DeleteTicketRequest.newBuilder()
            .setTicketId(ticketId.toString())
            .build()

        try {
            stub.deleteTicket(request)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override suspend fun deleteTicketByPlayer(playerId: UUID) {
        val request = DeleteTicketRequest.newBuilder()
            .setPlayerId(playerId.toString())
            .build()

        try {
            stub.deleteTicket(request)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override suspend fun updateTicketProperties(
        ticketId: UUID,
        properties: Map<String, String>,
        removedKeys: Set<String>,
    ): Ticket {
        val request = UpdateTicketPropertiesRequest.newBuilder()
            .setTicketId(ticketId.toString())
            .putAllProperties(properties)
            .addAllRemovedKeys(removedKeys)
            .build()

        try {
            return ProtoUtil.toTicket(stub.updateTicketProperties(request).ticket)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override suspend fun updateTicketPropertiesByPlayer(
        playerId: UUID,
        properties: Map<String, String>,
        removedKeys: Set<String>,
    ): Ticket {
        val request = UpdateTicketPropertiesRequest.newBuilder()
            .setPlayerId(playerId.toString())
            .putAllProperties(properties)
            .addAllRemovedKeys(removedKeys)
            .build()

        try {
            return ProtoUtil.toTicket(stub.updateTicketProperties(request).ticket)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override fun createTicketAsync(
        playerIds: List<UUID>,
        queueTypes: List<String>,
        properties: Map<String, String>,
    ): CompletableFuture<Ticket> = scope.future { createTicket(playerIds, queueTypes, properties) }

    override fun deleteTicketAsync(ticketId: UUID): CompletableFuture<Void?> = scope.future {
        deleteTicket(ticketId)
        null
    }

    override fun deleteTicketByPlayerAsync(playerId: UUID): CompletableFuture<Void?> = scope.future {
        deleteTicketByPlayer(playerId)
        null
    }

    override fun updateTicketPropertiesAsync(
        ticketId: UUID,
        properties: Map<String, String>,
        removedKeys: Set<String>,
    ): CompletableFuture<Ticket> = scope.future { updateTicketProperties(ticketId, properties, removedKeys) }

    override fun updateTicketPropertiesByPlayerAsync(
        playerId: UUID,
        properties: Map<String, String>,
        removedKeys: Set<String>,
    ): CompletableFuture<Ticket> = scope.future { updateTicketPropertiesByPlayer(playerId, properties, removedKeys) }

}

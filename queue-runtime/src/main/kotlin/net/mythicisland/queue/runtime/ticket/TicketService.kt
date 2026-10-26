package net.mythicisland.queue.runtime.ticket

import build.buf.gen.mythicisland.queue.v1.CreateTicketRequest
import build.buf.gen.mythicisland.queue.v1.CreateTicketResponse
import build.buf.gen.mythicisland.queue.v1.DeleteTicketRequest
import build.buf.gen.mythicisland.queue.v1.DeleteTicketResponse
import build.buf.gen.mythicisland.queue.v1.TicketDeleteReason
import build.buf.gen.mythicisland.queue.v1.TicketServiceGrpcKt
import build.buf.gen.mythicisland.queue.v1.createTicketResponse
import io.grpc.Status
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.match.MatchReconciler
import net.mythicisland.queue.runtime.type.QueueTypeRegistry
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import net.mythicisland.queue.shared.ticket.Ticket
import net.mythicisland.queue.shared.ticket.TicketState
import java.time.Instant
import java.util.UUID

class TicketService(
    private val types: QueueTypeRegistry,
    private val store: TicketStore,
    private val reconciler: MatchReconciler,
    private val events: EventPublisher,
) : TicketServiceGrpcKt.TicketServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(TicketService::class)

    override suspend fun createTicket(request: CreateTicketRequest): CreateTicketResponse {
        if (request.playerIdsCount == 0 || request.queueTypesCount == 0) {
            throw invalidArgument("A ticket needs at least one player and one queue type")
        }
        validateProperties(request.propertiesMap)

        val players = request.playerIdsList.map { parsePlayer(it) }.distinct()
        val queueTypes = request.queueTypesList.distinct().map { name ->
            types.get(name) ?: throw Status.NOT_FOUND.withDescription("Queue type $name does not exist").asException()
        }

        val exceeded = queueTypes.firstOrNull { players.size > it.match.effectivePartySize }
        if (exceeded != null) {
            throw invalidArgument("Queue type ${exceeded.name} allows at most ${exceeded.match.effectivePartySize} players per ticket")
        }

        val created = Ticket(
            id = UUID.randomUUID().toString(),
            playerIds = players,
            queueTypes = queueTypes.map { it.name },
            state = TicketState.SEARCHING,
            createdAt = Instant.now(),
            properties = request.propertiesMap.toMap(),
        )
        if (!store.add(created)) {
            throw Status.ALREADY_EXISTS.withDescription("One of the players already has a ticket").asException()
        }

        events.ticketCreated(created)
        logger.info(
            "Created ticket",
            Field("ticketId", created.id),
            Field("players", created.playerIds.size),
            Field("queueTypes", created.queueTypes),
        )
        return createTicketResponse { this.ticket = created.toProto() }
    }

    override suspend fun deleteTicket(request: DeleteTicketRequest): DeleteTicketResponse {
        val ticket = when (request.targetCase) {
            DeleteTicketRequest.TargetCase.TICKET_ID -> store.get(request.ticketId)
            DeleteTicketRequest.TargetCase.PLAYER_ID -> store.getByPlayer(parsePlayer(request.playerId))
            else -> throw invalidArgument("A ticket id or a player id is required")
        }

        if (ticket == null || !delete(ticket, TicketDeleteReason.TICKET_DELETE_REASON_CANCELLED)) {
            throw Status.NOT_FOUND.withDescription("There is no such ticket").asException()
        }
        return DeleteTicketResponse.getDefaultInstance()
    }

    suspend fun delete(ticket: Ticket, reason: TicketDeleteReason): Boolean {
        val removed = store.remove(ticket.id) ?: return false

        events.ticketDeleted(removed, reason)
        logger.info("Deleted ticket", Field("ticketId", removed.id), Field("reason", reason))
        reconciler.ticketLeft(removed)
        return true
    }

    private fun validateProperties(properties: Map<String, String>) {
        if (properties.size > MAX_PROPERTIES) {
            throw invalidArgument("A ticket has at most $MAX_PROPERTIES properties")
        }
        if (properties.keys.any { it.length > MAX_PROPERTY_KEY_LENGTH }) {
            throw invalidArgument("Property keys have at most $MAX_PROPERTY_KEY_LENGTH characters")
        }
        if (properties.values.any { it.length > MAX_PROPERTY_VALUE_LENGTH }) {
            throw invalidArgument("Property values have at most $MAX_PROPERTY_VALUE_LENGTH characters")
        }
    }

    private fun parsePlayer(id: String): UUID = try {
        UUID.fromString(id)
    } catch (_: IllegalArgumentException) {
        throw invalidArgument("$id is not a valid player uuid")
    }

    private fun invalidArgument(description: String) = Status.INVALID_ARGUMENT.withDescription(description).asException()

    private companion object {
        const val MAX_PROPERTIES = 32
        const val MAX_PROPERTY_KEY_LENGTH = 64
        const val MAX_PROPERTY_VALUE_LENGTH = 1024
    }

}

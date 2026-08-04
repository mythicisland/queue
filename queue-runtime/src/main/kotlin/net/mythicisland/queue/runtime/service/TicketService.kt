package net.mythicisland.queue.runtime.service

import build.buf.gen.mythicisland.queue.v2.*
import io.grpc.Status
import net.mythicisland.moonrise.common.extension.asUUID
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.repository.MatchRepository
import net.mythicisland.queue.runtime.repository.QueueTypeRepository
import net.mythicisland.queue.runtime.ticket.TicketStore
import net.mythicisland.queue.shared.match.Ticket
import org.apache.logging.log4j.LogManager
import java.time.Instant
import java.util.UUID

class TicketService(
    private val tickets: TicketStore,
    private val matches: MatchRepository,
    private val types: QueueTypeRepository,
    private val publisher: EventPublisher,
) : TicketServiceGrpcKt.TicketServiceCoroutineImplBase() {

    private val logger = LogManager.getLogger(TicketService::class.java)

    override suspend fun createTicket(request: CreateTicketRequest): CreateTicketResponse {
        val playerIds = request.playerIdsList.map { it.asUUID() }
        val queueTypes = request.queueTypesList.toList()

        if (playerIds.isEmpty()) {
            throw Status.INVALID_ARGUMENT
                .withDescription("A ticket needs at least one player")
                .asRuntimeException()
        }

        if (queueTypes.isEmpty()) {
            throw Status.INVALID_ARGUMENT
                .withDescription("A ticket needs at least one queue type")
                .asRuntimeException()
        }

        val unknown = queueTypes.filter { types.find(it) == null }
        if (unknown.isNotEmpty()) {
            logger.warn("Rejected ticket for players {}, unknown queue types {}", playerIds, unknown)
            throw Status.NOT_FOUND
                .withDescription("Unknown queue types: $unknown")
                .asRuntimeException()
        }

        val tooBig = queueTypes.mapNotNull { types.find(it) }.filter { playerIds.size > it.maxPlayers }
        if (tooBig.isNotEmpty()) {
            logger.warn("Rejected ticket for {} players, too big for {}", playerIds.size, tooBig.map { it.name })
            throw Status.FAILED_PRECONDITION
                .withDescription("Party of ${playerIds.size} players is too big for: ${tooBig.map { it.name }}")
                .asRuntimeException()
        }

        val ticket = Ticket(
            id = UUID.randomUUID(),
            playerIds = playerIds,
            queueTypes = queueTypes,
            state = TicketState.TICKET_STATE_SEARCHING,
            createdAt = Instant.now(),
        )

        if (!tickets.add(ticket)) {
            logger.warn("Rejected ticket for players {}, some of them are already queued", playerIds)
            throw Status.FAILED_PRECONDITION
                .withDescription("Some players are already queued")
                .asRuntimeException()
        }

        logger.info("Created ticket {} for {} players in {}", ticket.id, playerIds.size, queueTypes)
        publisher.publishTicketCreated(ticket)

        return createTicketResponse { this.ticket = ticket.toDefinition() }
    }

    override suspend fun deleteTicket(request: DeleteTicketRequest): DeleteTicketResponse {
        val ticket = when (request.targetCase) {
            DeleteTicketRequest.TargetCase.TICKET_ID -> tickets.get(request.ticketId.asUUID())
            DeleteTicketRequest.TargetCase.PLAYER_ID -> tickets.getByPlayer(request.playerId.asUUID())
            else -> throw Status.INVALID_ARGUMENT
                .withDescription("Either a ticket id or a player id is required")
                .asRuntimeException()
        } ?: throw Status.NOT_FOUND
            .withDescription("No ticket found for ${request.targetCase}")
            .asRuntimeException()

        tickets.remove(ticket.id)

        // The match keeps running with the players that are left, the reconciler
        // fails it once nobody is in it anymore.
        matches.removeTicket(ticket.id)

        logger.info("Deleted ticket {} with {} players", ticket.id, ticket.playerIds.size)
        publisher.publishTicketDeleted(ticket, TicketDeleteReason.TICKET_DELETE_REASON_CANCELLED)

        return deleteTicketResponse { }
    }

}

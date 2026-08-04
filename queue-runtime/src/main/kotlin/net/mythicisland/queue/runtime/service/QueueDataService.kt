package net.mythicisland.queue.runtime.service

import build.buf.gen.mythicisland.queue.v2.*
import io.grpc.Status
import net.mythicisland.moonrise.common.extension.asUUID
import net.mythicisland.queue.runtime.repository.MatchRepository
import net.mythicisland.queue.runtime.repository.QueueTypeRepository
import net.mythicisland.queue.runtime.ticket.TicketPool
import net.mythicisland.queue.runtime.ticket.TicketStore
import net.mythicisland.queue.shared.match.Match
import net.mythicisland.queue.shared.queue.QueueStats
import net.mythicisland.queue.shared.queue.QueueType

/**
 * Read only access to tickets, matches and the queue configuration.
 */
class QueueDataService(
    private val tickets: TicketStore,
    private val pool: TicketPool,
    private val matches: MatchRepository,
    private val types: QueueTypeRepository,
) : QueueDataServiceGrpcKt.QueueDataServiceCoroutineImplBase() {

    override suspend fun getTicket(request: GetTicketRequest): GetTicketResponse {
        val id = request.ticketId.asUUID()
        val ticket = tickets.get(id)
            ?: throw Status.NOT_FOUND
                .withDescription("Ticket '$id' not found")
                .asRuntimeException()

        return getTicketResponse { this.ticket = ticket.toDefinition() }
    }

    override suspend fun getTicketByPlayer(request: GetTicketByPlayerRequest): GetTicketByPlayerResponse {
        val playerId = request.playerId.asUUID()
        val ticket = tickets.getByPlayer(playerId)
            ?: throw Status.NOT_FOUND
                .withDescription("Player '$playerId' is not queued")
                .asRuntimeException()

        return getTicketByPlayerResponse { this.ticket = ticket.toDefinition() }
    }

    override suspend fun listTickets(request: ListTicketsRequest): ListTicketsResponse {
        val found = if (request.queueType.isEmpty()) {
            tickets.getAll()
        } else {
            tickets.getAll().filter { request.queueType in it.queueTypes }
        }

        return listTicketsResponse {
            this.tickets.addAll(found.map { it.toDefinition() })
        }
    }

    override suspend fun getMatch(request: GetMatchRequest): GetMatchResponse {
        val id = request.matchId.asUUID()
        val match = matches.get(id)
            ?: throw Status.NOT_FOUND
                .withDescription("Match '$id' not found")
                .asRuntimeException()

        return getMatchResponse { this.match = toDefinition(match) }
    }

    override suspend fun listMatches(request: ListMatchesRequest): ListMatchesResponse {
        val found = if (request.queueType.isEmpty()) {
            matches.getAll()
        } else {
            matches.getAllByType(request.queueType)
        }

        return listMatchesResponse {
            this.matches.addAll(found.map { toDefinition(it) })
        }
    }

    override suspend fun getQueueType(request: GetQueueTypeRequest): GetQueueTypeResponse {
        val type = types.find(request.name)
            ?: throw Status.NOT_FOUND
                .withDescription("Queue type '${request.name}' not found")
                .asRuntimeException()

        return getQueueTypeResponse { this.queueType = type.toDefinition() }
    }

    override suspend fun listQueueTypes(request: ListQueueTypesRequest): ListQueueTypesResponse {
        return listQueueTypesResponse {
            this.queueTypes.addAll(types.getAll().map(QueueType::toDefinition))
        }
    }

    override suspend fun getQueueStats(request: GetQueueStatsRequest): GetQueueStatsResponse {
        val type = types.find(request.queueType)
            ?: throw Status.NOT_FOUND
                .withDescription("Queue type '${request.queueType}' not found")
                .asRuntimeException()

        return getQueueStatsResponse { this.stats = statsOf(type).toDefinition() }
    }

    override suspend fun listQueueStats(request: ListQueueStatsRequest): ListQueueStatsResponse {
        return listQueueStatsResponse {
            this.stats.addAll(types.getAll().map { statsOf(it).toDefinition() })
        }
    }

    /**
     * Builds the protobuf of a match, resolving its tickets from the store.
     */
    private fun toDefinition(match: Match): build.buf.gen.mythicisland.queue.v2.Match {
        return match.toDefinition(tickets.getAll(match.ticketIds))
    }

    private fun statsOf(type: QueueType): QueueStats {
        return pool.stats(type.name, matches.getAllByType(type.name).size)
    }

}

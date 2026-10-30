package net.mythicisland.queue.runtime.data

import build.buf.gen.mythicisland.queue.v1.*
import io.grpc.Status
import net.mythicisland.queue.runtime.match.MatchStore
import net.mythicisland.queue.runtime.ticket.TicketStore
import net.mythicisland.queue.runtime.type.QueueTypeRegistry
import net.mythicisland.queue.shared.match.MatchState
import net.mythicisland.queue.shared.stats.QueueStats
import net.mythicisland.queue.shared.type.QueueType
import java.util.UUID

class QueueDataService(
    private val types: QueueTypeRegistry,
    private val ticketStore: TicketStore,
    private val matchStore: MatchStore,
) : QueueDataServiceGrpcKt.QueueDataServiceCoroutineImplBase() {

    override suspend fun getTicket(request: GetTicketRequest): GetTicketResponse {
        val found = ticketStore.get(request.ticketId) ?: throw notFound("Ticket ${request.ticketId} does not exist")
        return getTicketResponse { this.ticket = found.toProto() }
    }

    override suspend fun getTicketByPlayer(request: GetTicketByPlayerRequest): GetTicketByPlayerResponse {
        val player = try {
            UUID.fromString(request.playerId)
        } catch (_: IllegalArgumentException) {
            throw Status.INVALID_ARGUMENT.withDescription("${request.playerId} is not a valid player uuid").asException()
        }
        val found = ticketStore.getByPlayer(player) ?: throw notFound("Player ${request.playerId} has no ticket")
        return getTicketByPlayerResponse { this.ticket = found.toProto() }
    }

    override suspend fun listTickets(request: ListTicketsRequest): ListTicketsResponse {
        val found = ticketStore.all().filter { request.queueType.isEmpty() || request.queueType in it.queueTypes }
        return listTicketsResponse { this.tickets += found.map { it.toProto() } }
    }

    override suspend fun getMatch(request: GetMatchRequest): GetMatchResponse {
        val found = matchStore.get(request.matchId) ?: throw notFound("Match ${request.matchId} does not exist")
        return getMatchResponse { this.match = found.toProto() }
    }

    override suspend fun listMatches(request: ListMatchesRequest): ListMatchesResponse {
        val found = matchStore.all().filter {
            (request.queueType.isEmpty() || it.queueType == request.queueType) &&
                (request.serverId.isEmpty() || it.assignment?.serverId == request.serverId)
        }
        return listMatchesResponse { this.matches += found.map { it.toProto() } }
    }

    override suspend fun getQueueType(request: GetQueueTypeRequest): GetQueueTypeResponse {
        val found = types.get(request.name) ?: throw notFound("Queue type ${request.name} does not exist")
        return getQueueTypeResponse { this.queueType = found.toProto() }
    }

    override suspend fun listQueueTypes(request: ListQueueTypesRequest): ListQueueTypesResponse =
        listQueueTypesResponse { this.queueTypes += types.all().map { it.toProto() } }

    override suspend fun getQueueStats(request: GetQueueStatsRequest): GetQueueStatsResponse {
        val found = types.get(request.queueType) ?: throw notFound("Queue type ${request.queueType} does not exist")
        return getQueueStatsResponse { this.stats = stats(found).toProto() }
    }

    override suspend fun listQueueStats(request: ListQueueStatsRequest): ListQueueStatsResponse =
        listQueueStatsResponse { this.stats += types.all().map { stats(it).toProto() } }

    private fun stats(type: QueueType): QueueStats {
        val searching = ticketStore.searching(type.name)
        return QueueStats(
            queueType = type.name,
            searchingTickets = searching.size,
            searchingPlayers = searching.sumOf { it.getPlayerCount() },
            activeMatches = matchStore.all().count { it.queueType == type.name && it.state != MatchState.COMPLETED },
        )
    }

    private fun notFound(description: String) = Status.NOT_FOUND.withDescription(description).asException()

}

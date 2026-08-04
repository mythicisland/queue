package net.mythicisland.queue.api.extensions

import kotlinx.coroutines.future.await
import net.mythicisland.queue.api.data.QueueDataApi
import net.mythicisland.queue.api.match.Match
import net.mythicisland.queue.api.ticket.Ticket
import java.util.UUID

/**
 * Reads a single ticket.
 *
 * @param ticketId the unique ID of the ticket.
 * @return the ticket.
 */
suspend fun QueueDataApi.ticket(ticketId: UUID): Ticket {
    return getTicket(ticketId).await().ticket.toApi()
}

/**
 * Reads the ticket a player belongs to.
 *
 * @param playerId the UUID of the player.
 * @return the ticket the player is part of.
 */
suspend fun QueueDataApi.ticketOf(playerId: UUID): Ticket {
    return getTicketByPlayer(playerId).await().ticket.toApi()
}

/**
 * Reads every ticket in matchmaking.
 *
 * @param queueType only the tickets searching in this queue type, null for all of them.
 * @return the matching tickets.
 */
suspend fun QueueDataApi.tickets(queueType: String? = null): List<Ticket> {
    val response = if (queueType == null) listTickets() else listTickets(queueType)
    return response.await().ticketsList.map { it.toApi() }
}

/**
 * Reads a single match.
 *
 * @param matchId the unique ID of the match.
 * @return the match.
 */
suspend fun QueueDataApi.match(matchId: UUID): Match {
    return getMatch(matchId).await().match.toApi()
}

/**
 * Reads every match that has not finished yet.
 *
 * @param queueType only the matches of this queue type, null for all of them.
 * @return the matching matches.
 */
suspend fun QueueDataApi.matches(queueType: String? = null): List<Match> {
    val response = if (queueType == null) listMatches() else listMatches(queueType)
    return response.await().matchesList.map { it.toApi() }
}

/**
 * Reads the configuration of a queue type.
 *
 * There is no API type for the configuration, so this hands back the protobuf
 * message the runtime answered with.
 *
 * @param name the name of the queue type.
 * @return the queue type configuration.
 */
suspend fun QueueDataApi.queueType(name: String): build.buf.gen.mythicisland.queue.v2.QueueType {
    return getQueueType(name).await().queueType
}

/**
 * Reads every registered queue type.
 *
 * @return the configuration of all queue types.
 */
suspend fun QueueDataApi.queueTypes(): List<build.buf.gen.mythicisland.queue.v2.QueueType> {
    return listQueueTypes().await().queueTypesList
}

/**
 * Reads the live numbers of a queue type, for example how many players are
 * currently searching.
 *
 * @param queueType the name of the queue type.
 * @return the statistics of that queue type.
 */
suspend fun QueueDataApi.stats(queueType: String): build.buf.gen.mythicisland.queue.v2.QueueStats {
    return getQueueStats(queueType).await().stats
}

/**
 * Reads the live numbers of every queue type.
 *
 * @return the statistics of all queue types.
 */
suspend fun QueueDataApi.stats(): List<build.buf.gen.mythicisland.queue.v2.QueueStats> {
    return listQueueStats().await().statsList
}

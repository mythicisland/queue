package net.mythicisland.queue.api.data

import net.mythicisland.queue.api.match.Match
import net.mythicisland.queue.api.stats.QueueStats
import net.mythicisland.queue.api.ticket.Ticket
import net.mythicisland.queue.api.type.QueueType
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Reading tickets, matches, queue types and stats.
 */
interface QueueDataApi {

    /**
     * Returns the ticket with the given [ticketId], or `null` if there is none.
     */
    suspend fun getTicket(ticketId: UUID): Ticket?

    /**
     * Returns the ticket of the player with the given [playerId], or `null` if the player isn't queued.
     */
    suspend fun getTicketByPlayer(playerId: UUID): Ticket?

    /**
     * Returns all tickets, or only the ones queued for [queueType].
     */
    suspend fun listTickets(queueType: String? = null): List<Ticket>

    /**
     * Returns the match with the given [matchId], or `null` if there is none.
     */
    suspend fun getMatch(matchId: UUID): Match?

    /**
     * Returns all matches, optionally filtered by [queueType] and the server they run on.
     */
    suspend fun listMatches(queueType: String? = null, serverId: String? = null): List<Match>

    /**
     * Returns the queue type with the given [name], or `null` if there is none.
     */
    suspend fun getQueueType(name: String): QueueType?

    /**
     * Returns all queue types.
     */
    suspend fun listQueueTypes(): List<QueueType>

    /**
     * Returns the stats of [queueType], or `null` if there is no such queue type.
     */
    suspend fun getQueueStats(queueType: String): QueueStats?

    /**
     * Returns the stats of all queue types.
     */
    suspend fun listQueueStats(): List<QueueStats>

    /**
     * Same as [getTicket], but returns a [CompletableFuture].
     */
    fun getTicketAsync(ticketId: UUID): CompletableFuture<Ticket?>

    /**
     * Same as [getTicketByPlayer], but returns a [CompletableFuture].
     */
    fun getTicketByPlayerAsync(playerId: UUID): CompletableFuture<Ticket?>

    /**
     * Same as [listTickets], but returns a [CompletableFuture].
     */
    fun listTicketsAsync(queueType: String?): CompletableFuture<List<Ticket>>

    /**
     * Same as [listTickets], but returns a [CompletableFuture].
     */
    fun listTicketsAsync(): CompletableFuture<List<Ticket>> = listTicketsAsync(null)

    /**
     * Same as [getMatch], but returns a [CompletableFuture].
     */
    fun getMatchAsync(matchId: UUID): CompletableFuture<Match?>

    /**
     * Same as [listMatches], but returns a [CompletableFuture].
     */
    fun listMatchesAsync(queueType: String?, serverId: String?): CompletableFuture<List<Match>>

    /**
     * Same as [listMatches], but returns a [CompletableFuture].
     */
    fun listMatchesAsync(): CompletableFuture<List<Match>> = listMatchesAsync(null, null)

    /**
     * Same as [getQueueType], but returns a [CompletableFuture].
     */
    fun getQueueTypeAsync(name: String): CompletableFuture<QueueType?>

    /**
     * Same as [listQueueTypes], but returns a [CompletableFuture].
     */
    fun listQueueTypesAsync(): CompletableFuture<List<QueueType>>

    /**
     * Same as [getQueueStats], but returns a [CompletableFuture].
     */
    fun getQueueStatsAsync(queueType: String): CompletableFuture<QueueStats?>

    /**
     * Same as [listQueueStats], but returns a [CompletableFuture].
     */
    fun listQueueStatsAsync(): CompletableFuture<List<QueueStats>>

}

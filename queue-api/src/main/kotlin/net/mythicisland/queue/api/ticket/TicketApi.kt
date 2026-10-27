package net.mythicisland.queue.api.ticket

import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Creating and deleting tickets.
 */
interface TicketApi {

    /**
     * Creates a ticket for [playerIds] in all of [queueTypes] and returns it.
     *
     * Fails with `ALREADY_EXISTS` if one of the players already has a ticket.
     */
    suspend fun createTicket(
        playerIds: List<UUID>,
        queueTypes: List<String>,
        properties: Map<String, String> = emptyMap(),
    ): Ticket

    /**
     * Creates a ticket for [playerIds] in [queueType] and returns it.
     */
    suspend fun createTicket(
        playerIds: List<UUID>,
        queueType: String,
        properties: Map<String, String> = emptyMap(),
    ): Ticket = createTicket(playerIds, listOf(queueType), properties)

    /**
     * Creates a ticket for a single player in all of [queueTypes] and returns it.
     */
    suspend fun createTicket(
        playerId: UUID,
        queueTypes: List<String>,
        properties: Map<String, String> = emptyMap(),
    ): Ticket = createTicket(listOf(playerId), queueTypes, properties)

    /**
     * Creates a ticket for a single player in [queueType] and returns it.
     */
    suspend fun createTicket(
        playerId: UUID,
        queueType: String,
        properties: Map<String, String> = emptyMap(),
    ): Ticket = createTicket(listOf(playerId), listOf(queueType), properties)

    /**
     * Deletes the ticket with the given [ticketId].
     */
    suspend fun deleteTicket(ticketId: UUID)

    /**
     * Deletes the ticket of the player with the given [playerId].
     */
    suspend fun deleteTicketByPlayer(playerId: UUID)

    /**
     * Sets [properties] and removes [removedKeys] on the ticket with the given [ticketId] and returns it.
     * All other properties stay as they are, a key in both is set.
     *
     * Fails with `NOT_FOUND` if there is no such ticket, and with `INVALID_ARGUMENT` if the properties exceed their limits.
     */
    suspend fun updateTicketProperties(
        ticketId: UUID,
        properties: Map<String, String> = emptyMap(),
        removedKeys: Set<String> = emptySet(),
    ): Ticket

    /**
     * Same as [updateTicketProperties], for the ticket of the player with the given [playerId].
     */
    suspend fun updateTicketPropertiesByPlayer(
        playerId: UUID,
        properties: Map<String, String> = emptyMap(),
        removedKeys: Set<String> = emptySet(),
    ): Ticket

    /**
     * Same as [createTicket], but returns a [CompletableFuture].
     */
    fun createTicketAsync(
        playerIds: List<UUID>,
        queueTypes: List<String>,
        properties: Map<String, String>,
    ): CompletableFuture<Ticket>

    /**
     * Same as [createTicket], but returns a [CompletableFuture].
     */
    fun createTicketAsync(playerIds: List<UUID>, queueTypes: List<String>): CompletableFuture<Ticket> =
        createTicketAsync(playerIds, queueTypes, emptyMap())

    /**
     * Same as [createTicket], but returns a [CompletableFuture].
     */
    fun createTicketAsync(
        playerIds: List<UUID>,
        queueType: String,
        properties: Map<String, String>,
    ): CompletableFuture<Ticket> = createTicketAsync(playerIds, listOf(queueType), properties)

    /**
     * Same as [createTicket], but returns a [CompletableFuture].
     */
    fun createTicketAsync(playerIds: List<UUID>, queueType: String): CompletableFuture<Ticket> =
        createTicketAsync(playerIds, listOf(queueType), emptyMap())

    /**
     * Same as [createTicket], but returns a [CompletableFuture].
     */
    fun createTicketAsync(
        playerId: UUID,
        queueTypes: List<String>,
        properties: Map<String, String>,
    ): CompletableFuture<Ticket> = createTicketAsync(listOf(playerId), queueTypes, properties)

    /**
     * Same as [createTicket], but returns a [CompletableFuture].
     */
    fun createTicketAsync(playerId: UUID, queueTypes: List<String>): CompletableFuture<Ticket> =
        createTicketAsync(listOf(playerId), queueTypes, emptyMap())

    /**
     * Same as [createTicket], but returns a [CompletableFuture].
     */
    fun createTicketAsync(
        playerId: UUID,
        queueType: String,
        properties: Map<String, String>,
    ): CompletableFuture<Ticket> = createTicketAsync(listOf(playerId), listOf(queueType), properties)

    /**
     * Same as [createTicket], but returns a [CompletableFuture].
     */
    fun createTicketAsync(playerId: UUID, queueType: String): CompletableFuture<Ticket> =
        createTicketAsync(listOf(playerId), listOf(queueType), emptyMap())

    /**
     * Same as [deleteTicket], but returns a [CompletableFuture].
     */
    fun deleteTicketAsync(ticketId: UUID): CompletableFuture<Void?>

    /**
     * Same as [deleteTicketByPlayer], but returns a [CompletableFuture].
     */
    fun deleteTicketByPlayerAsync(playerId: UUID): CompletableFuture<Void?>

    /**
     * Same as [updateTicketProperties], but returns a [CompletableFuture].
     */
    fun updateTicketPropertiesAsync(
        ticketId: UUID,
        properties: Map<String, String>,
        removedKeys: Set<String>,
    ): CompletableFuture<Ticket>

    /**
     * Same as [updateTicketPropertiesByPlayer], but returns a [CompletableFuture].
     */
    fun updateTicketPropertiesByPlayerAsync(
        playerId: UUID,
        properties: Map<String, String>,
        removedKeys: Set<String>,
    ): CompletableFuture<Ticket>

}

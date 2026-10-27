package net.mythicisland.queue.api.ticket

import java.util.UUID

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

}

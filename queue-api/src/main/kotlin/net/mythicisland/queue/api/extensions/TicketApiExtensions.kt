package net.mythicisland.queue.api.extensions

import kotlinx.coroutines.future.await
import net.mythicisland.queue.api.builders.TicketBuilder
import net.mythicisland.queue.api.ticket.Ticket
import net.mythicisland.queue.api.ticket.TicketApi
import java.util.UUID

/**
 * Puts a player or party into matchmaking.
 *
 * Suspends until the runtime accepted the ticket and answers with the created
 * [Ticket] instead of the raw protobuf response.
 *
 * ```
 * val ticket = api.ticket().create {
 *     party(leader, member)
 *     queues("battle", "skywars")
 * }
 * ```
 *
 * @param block describes the ticket.
 * @return the created ticket.
 * @throws IllegalArgumentException if no player or no queue type was added.
 */
suspend fun TicketApi.create(block: TicketBuilder.() -> Unit): Ticket {
    val builder = TicketBuilder().apply(block)
    builder.validate()

    return createTicket(builder.playerIds(), builder.queueTypes()).await().ticket.toApi()
}

/**
 * Puts a single player into matchmaking.
 *
 * @param playerId the UUID of the player.
 * @param queueTypes the queue types to search in.
 * @return the created ticket.
 */
suspend fun TicketApi.create(playerId: UUID, vararg queueTypes: String): Ticket {
    return create {
        player(playerId)
        queues(*queueTypes)
    }
}

/**
 * Removes a ticket from matchmaking.
 *
 * @param ticketId the unique ID of the ticket.
 */
suspend fun TicketApi.delete(ticketId: UUID) {
    deleteTicket(ticketId).await()
}

/**
 * Removes the ticket a player belongs to, including the rest of their party.
 *
 * @param playerId the UUID of the player.
 */
suspend fun TicketApi.deleteByPlayer(playerId: UUID) {
    deleteTicketByPlayer(playerId).await()
}

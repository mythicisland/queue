package net.mythicisland.queue.api.event.ticket

import net.mythicisland.queue.api.ticket.Ticket

/**
 * Fired when a ticket was created.
 */
data class TicketCreatedEvent(
    val ticket: Ticket,
)

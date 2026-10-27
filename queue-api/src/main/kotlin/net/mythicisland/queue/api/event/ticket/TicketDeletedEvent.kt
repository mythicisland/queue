package net.mythicisland.queue.api.event.ticket

import net.mythicisland.queue.api.ticket.Ticket
import net.mythicisland.queue.api.ticket.TicketDeleteReason

/**
 * Fired when a ticket was deleted.
 */
data class TicketDeletedEvent(
    val ticket: Ticket,
    val reason: TicketDeleteReason,
)

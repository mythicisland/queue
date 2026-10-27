package net.mythicisland.queue.api.event.ticket

import net.mythicisland.queue.api.ticket.Ticket

/**
 * Fired when the properties of a ticket changed from [previousProperties] to the ones it has now.
 */
data class TicketUpdatedEvent(
    val ticket: Ticket,
    val previousProperties: Map<String, String>,
)

package net.mythicisland.queue.api.event.ticket

import net.mythicisland.queue.api.ticket.Ticket
import net.mythicisland.queue.api.ticket.TicketState

/**
 * Fired when a ticket changed from [previousState] to the state it has now.
 */
data class TicketStateChangedEvent(
    val ticket: Ticket,
    val previousState: TicketState,
)

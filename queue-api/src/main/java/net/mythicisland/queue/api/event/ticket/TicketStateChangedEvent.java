package net.mythicisland.queue.api.event.ticket;

import net.mythicisland.queue.api.ticket.Ticket;
import net.mythicisland.queue.api.ticket.TicketState;

/**
 * Event fired when a ticket transitions from one state to another.
 *
 * @param ticket the ticket, already carrying its new state
 * @param previousState the state the ticket was in before
 */
public record TicketStateChangedEvent(
        Ticket ticket,
        TicketState previousState
) {
}

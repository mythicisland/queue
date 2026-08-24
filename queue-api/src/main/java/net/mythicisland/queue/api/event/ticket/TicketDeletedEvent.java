package net.mythicisland.queue.api.event.ticket;

import net.mythicisland.queue.api.ticket.Ticket;
import net.mythicisland.queue.api.ticket.TicketDeleteReason;

/**
 * Event fired when a ticket leaves matchmaking.
 *
 * @param ticket the deleted ticket
 * @param reason why the ticket was deleted
 */
public record TicketDeletedEvent(
        Ticket ticket,
        TicketDeleteReason reason
) {
}

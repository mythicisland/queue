package net.mythicisland.queue.api.event.ticket;

import net.mythicisland.queue.api.ticket.Ticket;

/**
 * Event fired when a player or party entered matchmaking.
 *
 * @param ticket the created ticket
 */
public record TicketCreatedEvent(Ticket ticket) { }
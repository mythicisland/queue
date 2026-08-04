package net.mythicisland.queue.api.ticket;

/**
 * Represents the current state of a ticket.
 */
public enum TicketState {

    /**
     * The ticket is waiting in one or more queue types and is not part of a match yet.
     */
    SEARCHING,

    /**
     * The ticket is part of a match that is waiting for a server or counting down.
     */
    MATCHED,

    /**
     * A server was allocated and the players are being transferred to it.
     */
    ASSIGNED;

}

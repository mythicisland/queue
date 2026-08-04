package net.mythicisland.queue.api.ticket;

/**
 * Represents why a ticket left matchmaking.
 */
public enum TicketDeleteReason {

    /**
     * The player or party left the queue on purpose.
     */
    CANCELLED,

    /**
     * The players were transferred to their game server, matchmaking is done.
     */
    TRANSFERRED,

    /**
     * The ticket was dropped, for example because the players went offline.
     */
    EXPIRED;

}

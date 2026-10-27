package net.mythicisland.queue.api.ticket

/**
 * The current state of a ticket.
 */
enum class TicketState {

    /**
     * In the queue, not picked yet.
     */
    SEARCHING,

    /**
     * Part of a match that doesn't have a server yet.
     */
    MATCHED,

    /**
     * Has a server, countdown is running.
     */
    ASSIGNED;

}

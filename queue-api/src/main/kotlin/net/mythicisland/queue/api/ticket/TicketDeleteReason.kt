package net.mythicisland.queue.api.ticket

/**
 * Why a ticket was deleted.
 */
enum class TicketDeleteReason {

    /**
     * The ticket was deleted through the API.
     */
    CANCELLED,

    /**
     * The players got transferred to their match.
     */
    TRANSFERRED,

    /**
     * One of the players disconnected.
     */
    DISCONNECTED;

}

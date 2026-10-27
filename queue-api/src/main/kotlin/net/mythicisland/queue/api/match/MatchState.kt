package net.mythicisland.queue.api.match

/**
 * The current state of a match.
 */
enum class MatchState {

    /**
     * Waiting for a server.
     */
    ALLOCATING,

    /**
     * Offered to a shared server, waiting for accept or reject.
     */
    OFFERED,

    /**
     * Server is set, players get transferred when it hits zero.
     */
    COUNTDOWN,

    /**
     * Players are being transferred.
     */
    TRANSFERRING,

    /**
     * Players are on the server.
     */
    COMPLETED,

    /**
     * No server in time, or the server stopped.
     */
    FAILED,

    /**
     * Not enough players left before the transfer.
     */
    CANCELLED;

}

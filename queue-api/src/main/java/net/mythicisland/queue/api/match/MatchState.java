package net.mythicisland.queue.api.match;

/**
 * Represents the current state of a match.
 */
public enum MatchState {

    /**
     * A game server is being searched or started for this match.
     */
    ALLOCATING,

    /**
     * The server is ready and the match is counting down before the transfer.
     */
    COUNTDOWN,

    /**
     * The players are being transferred to the server.
     */
    TRANSFERRING,

    /**
     * Every player was transferred, the match is handed over to the game server.
     */
    COMPLETED,

    /**
     * No server could be allocated, the tickets went back to searching.
     */
    FAILED;

}

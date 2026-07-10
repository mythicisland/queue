package net.mythicisland.queue.api.queue;

/**
 * Represents the current status of a queue.
 */
public enum QueueStatus {

    /**
     * The queue does not have enough players to start a game.
     */
    NOT_ENOUGH_PLAYERS,

    /**
     * Minimum player count has been reached, and the pre-game countdown has started.
     */
    WAITING_COUNTDOWN,

    /**
     * The system is looking for an available game server for this queue.
     */
    SEARCHING_SERVER,

    /**
     * A request for a server has been made, but no server has been assigned yet.
     */
    WAITING_FOR_SERVER,

    /**
     * A game server has been successfully assigned and is ready to accept players.
     */
    SERVER_READY,

    /**
     * The final countdown before players are teleported.
     */
    COUNTDOWN,

    /**
     * Players are currently being teleported to the assigned game server.
     */
    TELEPORTING,

    /**
     * The queue lifecycle has been completed successfully.
     */
    FINISHED;

}

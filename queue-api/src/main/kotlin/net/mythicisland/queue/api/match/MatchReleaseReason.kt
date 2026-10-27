package net.mythicisland.queue.api.match

/**
 * Why a match was released.
 */
enum class MatchReleaseReason {

    /**
     * The game server released it through the API.
     */
    RELEASED,

    /**
     * The game server stopped.
     */
    SERVER_STOPPED;

}

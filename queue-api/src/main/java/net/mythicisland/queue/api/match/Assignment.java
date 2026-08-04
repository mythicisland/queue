package net.mythicisland.queue.api.match;

/**
 * The game server a match was allocated to.
 *
 * @param serverId the unique ID of the server
 * @param serverName the name used to connect players, for example battle-1
 */
public record Assignment(
        String serverId,
        String serverName
) {
}

package net.mythicisland.queue.api.match

/**
 * The server a match runs on.
 *
 * [serverName] is what players get connected to, e.g. `bedwars-1`.
 */
data class Assignment(
    val serverId: String,
    val serverName: String,
)

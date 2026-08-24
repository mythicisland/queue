package net.mythicisland.queue.shared.match

import build.buf.gen.mythicisland.queue.v2.assignment

/**
 * The server a match was allocated to.
 *
 * @param serverId the unique id of the simplecloud server.
 * @param serverName the name used to connect players, (e.g. battle-1)
 */
data class Assignment(
    val serverId: String,
    val serverName: String,
) {

    fun toDefinition(): build.buf.gen.mythicisland.queue.v2.Assignment {
        return assignment {
            serverId = this@Assignment.serverId
            serverName = this@Assignment.serverName
        }
    }

}

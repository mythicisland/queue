package net.mythicisland.queue.shared.match

import build.buf.gen.mythicisland.queue.v2.assignment

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

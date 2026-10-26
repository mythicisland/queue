package net.mythicisland.queue.shared.match

import build.buf.gen.mythicisland.queue.v1.assignment
import build.buf.gen.mythicisland.queue.v1.Assignment as AssignmentProto

data class Assignment(
    val serverId: String,
    val serverName: String,
) {

    fun toProto(): AssignmentProto = assignment {
        serverId = this@Assignment.serverId
        serverName = this@Assignment.serverName
    }

    companion object {
        fun fromProto(proto: AssignmentProto): Assignment = Assignment(proto.serverId, proto.serverName)
    }

}

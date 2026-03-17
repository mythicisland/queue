package net.mythicisland.queue.shared.queue

import app.simplecloud.api.server.Server
import build.buf.gen.mythicisland.queue.v1.QueueStatus
import java.util.UUID

data class Queue(
    val id: UUID,
    val type: String,
    var status: QueueStatus,
    val players: MutableList<UUID>,
    val capacity: Long = 0,
    var server: Server? = null,
) {
    /** Remaining waiting countdown in milliseconds. */
    var waitingCountdownRemaining: Long = 0

    /** Remaining game countdown in milliseconds. */
    var countdownRemaining: Long = 0

    fun toDefinition() : build.buf.gen.mythicisland.queue.v1.Queue {
        return build.buf.gen.mythicisland.queue.v1.Queue.newBuilder()
            .setUniqueId(id.toString())
            .setType(type)
            .setStatus(status)
            .addAllPlayerIds(players.map { it.toString() })
            .build()
    }

    companion object {
        fun fromDefinition(definition: build.buf.gen.mythicisland.queue.v1.Queue): Queue {
            return Queue(
                id = UUID.fromString(definition.uniqueId),
                type = definition.type,
                status = definition.status,
                players = definition.playerIdsList.map { UUID.fromString(it) }.toMutableList(),
            )
        }
    }
}
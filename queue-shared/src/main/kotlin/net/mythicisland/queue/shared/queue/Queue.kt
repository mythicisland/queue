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
    /** Absolute timestamp (ms) at which the waiting countdown ends, or null if not active. */
    var waitingCountdownEndsAt: Long? = null

    /** Absolute timestamp (ms) at which the game countdown ends, or null if not active. */
    var countdownEndsAt: Long? = null

    val waitingCountdownRemaining: Long
        get() = waitingCountdownEndsAt?.let { (it - System.currentTimeMillis()).coerceAtLeast(0) } ?: 0

    val countdownRemaining: Long
        get() = countdownEndsAt?.let { (it - System.currentTimeMillis()).coerceAtLeast(0) } ?: 0

    fun toDefinition() : build.buf.gen.mythicisland.queue.v1.Queue {
        return build.buf.gen.mythicisland.queue.v1.Queue.newBuilder()
            .setUniqueId(id.toString())
            .setType(type)
            .setStatus(status)
            .addAllPlayerIds(players.map { it.toString() })
            .build()
    }

}
package net.mythicisland.queue.shared.match

import build.buf.gen.mythicisland.queue.v2.MatchState
import build.buf.gen.mythicisland.queue.v2.match
import net.mythicisland.common.util.protobuf.toTimestamp
import java.time.Instant
import java.util.UUID

data class Match(
    val id: UUID,
    val queueType: String,
    val ticketIds: List<UUID>,
    val state: MatchState,
    val createdAt: Instant,
    val assignment: Assignment? = null,
    val countdownEndsAt: Instant? = null,
) {

    fun toDefinition(tickets: List<Ticket>): build.buf.gen.mythicisland.queue.v2.Match {
        return match {
            id = this@Match.id.toString()
            queueType = this@Match.queueType
            this.tickets.addAll(tickets.map { it.toDefinition() })
            state = this@Match.state
            createdAt = this@Match.createdAt.toTimestamp()
            this@Match.assignment?.let { assignment = it.toDefinition() }
            this@Match.countdownEndsAt?.let { countdownEndTime = it.toTimestamp() }
        }
    }

}

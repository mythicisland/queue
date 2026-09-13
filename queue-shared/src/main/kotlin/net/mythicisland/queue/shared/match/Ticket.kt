package net.mythicisland.queue.shared.match

import build.buf.gen.mythicisland.queue.v2.TicketState
import build.buf.gen.mythicisland.queue.v2.ticket
import net.mythicisland.common.util.protobuf.toTimestamp
import java.time.Instant
import java.util.UUID

data class Ticket(
    val id: UUID,
    val playerIds: List<UUID>,
    val queueTypes: List<String>,
    val state: TicketState,
    val createdAt: Instant,
    val matchId: UUID? = null,
    val assignment: Assignment? = null,
    val countdownEndsAt: Instant? = null,
) {

    fun toDefinition(): build.buf.gen.mythicisland.queue.v2.Ticket {
        return ticket {
            id = this@Ticket.id.toString()
            playerIds.addAll(this@Ticket.playerIds.map(UUID::toString))
            queueTypes.addAll(this@Ticket.queueTypes)
            state = this@Ticket.state
            createdAt = this@Ticket.createdAt.toTimestamp()
            this@Ticket.matchId?.let { matchId = it.toString() }
            this@Ticket.assignment?.let { assignment = it.toDefinition() }
            this@Ticket.countdownEndsAt?.let { countdownEndTime = it.toTimestamp() }
        }
    }

    fun asSearching(): Ticket {
        return copy(
            state = TicketState.TICKET_STATE_SEARCHING,
            matchId = null,
            assignment = null,
            countdownEndsAt = null,
        )
    }

}

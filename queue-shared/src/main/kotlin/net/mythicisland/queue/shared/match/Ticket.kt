package net.mythicisland.queue.shared.match

import build.buf.gen.mythicisland.queue.v2.TicketState
import build.buf.gen.mythicisland.queue.v2.ticket
import net.mythicisland.queue.shared.protobuf.toTimestamp
import java.time.Instant
import java.util.UUID

/**
 * A ticket is a single player or a party that wants to play.
 *
 * @param id the unique id of this ticket.
 * @param playerIds the players behind this ticket, one entry means solo.
 * @param queueTypes the queue types this ticket is searching in.
 * @param state the current state of this ticket.
 * @param createdAt when the ticket entered matchmaking.
 * @param matchId the match this ticket was put into, null while searching.
 * @param assignment the server to connect to, null until one was allocated.
 * @param countdownEndsAt when the players get transferred, mirrored from the match.
 */
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

    val playerCount: Int
        get() = playerIds.size

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

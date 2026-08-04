package net.mythicisland.queue.shared.match

import build.buf.gen.mythicisland.queue.v2.MatchState
import build.buf.gen.mythicisland.queue.v2.match
import net.mythicisland.queue.shared.protobuf.toTimestamp
import java.time.Instant
import java.util.UUID

/**
 * A match is a set of tickets that will play together on one server.
 *
 * Only the ticket ids are stored, the tickets themselves live in the ticket
 * store. That way there is exactly one place holding the ticket state.
 *
 * @param id the unique id of this match.
 * @param queueType the queue type this match was created for.
 * @param ticketIds the tickets forming this match.
 * @param state the current state of this match.
 * @param createdAt when the match was formed.
 * @param assignment the allocated server, null while allocating.
 * @param countdownEndsAt when the players get transferred, null until a server is there.
 */
data class Match(
    val id: UUID,
    val queueType: String,
    val ticketIds: List<UUID>,
    val state: MatchState,
    val createdAt: Instant,
    val assignment: Assignment? = null,
    val countdownEndsAt: Instant? = null,
) {

    /**
     * Builds the protobuf representation of this match.
     *
     * @param tickets the tickets of this match, resolved from the ticket store.
     */
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

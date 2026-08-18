package net.mythicisland.queue.runtime.event

import build.buf.gen.mythicisland.queue.v2.*
import io.nats.client.Connection
import net.mythicisland.common.nats.Publisher
import net.mythicisland.queue.shared.match.Match
import net.mythicisland.queue.shared.match.Ticket
import net.mythicisland.queue.shared.nats.Subjects
import java.util.UUID

/**
 * Publishes ticket and match events to NATS.
 */
class EventPublisher(
    connection: Connection
) : Publisher(connection) {

    /**
     * Publishes a [TicketCreatedEvent] when a player or party entered matchmaking.
     *
     * @param ticket the created ticket.
     */
    fun publishTicketCreated(ticket: Ticket) {
        val event = ticketCreatedEvent {
            this.ticket = ticket.toDefinition()
        }

        publish(Subjects.TICKET_CREATED, event)
    }

    /**
     * Publishes a [TicketStateChangedEvent] when a ticket moved to a new state.
     *
     * @param ticket the ticket in its new state.
     * @param previousState the state the ticket was in before.
     */
    fun publishTicketStateChanged(ticket: Ticket, previousState: TicketState) {
        val event = ticketStateChangedEvent {
            this.ticket = ticket.toDefinition()
            this.previousState = previousState
        }

        publish(Subjects.TICKET_STATE_CHANGED, event)
    }

    /**
     * Publishes a [TicketDeletedEvent] when a ticket left matchmaking.
     *
     * @param ticket the deleted ticket.
     * @param reason why the ticket was deleted.
     */
    fun publishTicketDeleted(ticket: Ticket, reason: TicketDeleteReason) {
        val event = ticketDeletedEvent {
            this.ticket = ticket.toDefinition()
            this.reason = reason
        }

        publish(Subjects.TICKET_DELETED, event)
    }

    /**
     * Publishes a [MatchCreatedEvent] when enough tickets were found for a match.
     *
     * @param match the created match.
     * @param tickets the tickets forming the match.
     */
    fun publishMatchCreated(match: Match, tickets: List<Ticket>) {
        val event = matchCreatedEvent {
            this.match = match.toDefinition(tickets)
        }

        publish(Subjects.MATCH_CREATED, event)
    }

    /**
     * Publishes a [MatchStateChangedEvent] when a match moved to a new state.
     *
     * @param match the match in its new state.
     * @param tickets the tickets forming the match.
     * @param previousState the state the match was in before.
     */
    fun publishMatchStateChanged(match: Match, tickets: List<Ticket>, previousState: MatchState) {
        val event = matchStateChangedEvent {
            this.match = match.toDefinition(tickets)
            this.previousState = previousState
        }

        publish(Subjects.MATCH_STATE_CHANGED, event)
    }

    /**
     * Publishes a [MatchTransferredEvent] after the players were sent to their server.
     *
     * @param match the transferred match.
     * @param tickets the tickets forming the match.
     * @param transferredPlayerIds the players that actually made it onto the server.
     */
    fun publishMatchTransferred(match: Match, tickets: List<Ticket>, transferredPlayerIds: List<UUID>) {
        val event = matchTransferredEvent {
            this.match = match.toDefinition(tickets)
            this.transferredPlayerIds.addAll(transferredPlayerIds.map(UUID::toString))
        }

        publish(Subjects.MATCH_TRANSFERRED, event)
    }

}

package net.mythicisland.queue.runtime.event

import build.buf.gen.mythicisland.queue.v1.*
import com.google.protobuf.Message
import io.nats.client.Connection
import net.mythicisland.queue.shared.event.Subjects
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import net.mythicisland.queue.shared.match.Match
import net.mythicisland.queue.shared.match.MatchState
import net.mythicisland.queue.shared.ticket.Ticket
import net.mythicisland.queue.shared.ticket.TicketState
import java.util.UUID

class EventPublisher(private val nats: Connection) {

    private val logger = LoggerFactory.getLogger(EventPublisher::class)

    fun ticketCreated(ticket: Ticket) = publish(
        Subjects.TICKET_CREATED,
        ticketCreatedEvent { this.ticket = ticket.toProto() },
    )

    fun ticketStateChanged(ticket: Ticket, previous: TicketState) = publish(
        Subjects.TICKET_STATE_CHANGED,
        ticketStateChangedEvent {
            this.ticket = ticket.toProto()
            this.previousState = previous.toProto()
        },
    )

    fun ticketUpdated(ticket: Ticket, previousProperties: Map<String, String>) = publish(
        Subjects.TICKET_UPDATED,
        ticketUpdatedEvent {
            this.ticket = ticket.toProto()
            this.previousProperties.putAll(previousProperties)
        },
    )

    fun ticketDeleted(ticket: Ticket, reason: TicketDeleteReason) = publish(
        Subjects.TICKET_DELETED,
        ticketDeletedEvent {
            this.ticket = ticket.toProto()
            this.reason = reason
        },
    )

    fun matchCreated(match: Match) = publishMatch(
        match,
        Subjects.MATCH_CREATED,
        matchCreatedEvent { this.match = match.toProto() },
    )

    fun matchStateChanged(match: Match, previous: MatchState) = publishMatch(
        match,
        Subjects.MATCH_STATE_CHANGED,
        matchStateChangedEvent {
            this.match = match.toProto()
            this.previousState = previous.toProto()
        },
    )

    fun matchTransferred(match: Match, transferred: List<UUID>, missing: List<UUID>) = publishMatch(
        match,
        Subjects.MATCH_TRANSFERRED,
        matchTransferredEvent {
            this.match = match.toProto()
            this.transferredPlayerIds += transferred.map(UUID::toString)
            this.missingPlayerIds += missing.map(UUID::toString)
        },
    )

    fun matchReleased(match: Match, reason: MatchReleaseReason) = publishMatch(
        match,
        Subjects.MATCH_RELEASED,
        matchReleasedEvent {
            this.match = match.toProto()
            this.reason = reason
        },
    )

    private fun publishMatch(match: Match, subject: String, event: Message) {
        publish(subject, event)
        val assignment = match.assignment ?: return
        publish(Subjects.server(assignment.serverId, subject), event)
    }

    private fun publish(subject: String, event: Message) {
        try {
            nats.publish(subject, event.toByteArray())
        } catch (e: Exception) {
            logger.error("Failed to publish event", Field("subject", subject), Field.error(e))
        }
    }

}

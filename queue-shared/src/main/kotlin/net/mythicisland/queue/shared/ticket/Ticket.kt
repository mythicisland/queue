package net.mythicisland.queue.shared.ticket

import build.buf.gen.mythicisland.queue.v1.ticket
import com.google.protobuf.timestamp
import net.mythicisland.queue.shared.match.Assignment
import net.mythicisland.queue.shared.type.QueueType
import java.time.Instant
import java.util.UUID
import build.buf.gen.mythicisland.queue.v1.Ticket as TicketProto

data class Ticket(
    val id: String,
    val playerIds: List<UUID>,
    val queueTypes: List<String>,
    val state: TicketState,
    val createdAt: Instant,
    val matchId: String? = null,
    val assignment: Assignment? = null,
    val countdownEndTime: Instant? = null,
    val properties: Map<String, String> = emptyMap(),
    val failures: Map<String, Int> = emptyMap(),
) {

    fun getPlayerCount(): Int = playerIds.size

    fun matched(matchId: String): Ticket = copy(state = TicketState.MATCHED, matchId = matchId)

    fun assigned(assignment: Assignment, countdownEnd: Instant): Ticket =
        copy(state = TicketState.ASSIGNED, assignment = assignment, countdownEndTime = countdownEnd)

    fun searching(): Ticket = copy(state = TicketState.SEARCHING, matchId = null, assignment = null, countdownEndTime = null)

    fun failed(queueType: String): Ticket = copy(failures = failures + (queueType to (failures[queueType] ?: 0) + 1))

    fun isSearchingIn(type: QueueType, now: Instant): Boolean {
        val maxWait = type.match.maxWait
        if (maxWait != null && !now.isBefore(createdAt.plus(maxWait))) {
            return false
        }
        val maxFailures = type.match.maxFailures
        return maxFailures == null || (failures[type.name] ?: 0) < maxFailures
    }

    fun isExpired(types: List<QueueType>, now: Instant): Boolean = types.none { isSearchingIn(it, now) }

    fun toProto(): TicketProto = ticket {
        id = this@Ticket.id
        playerIds += this@Ticket.playerIds.map(UUID::toString)
        queueTypes += this@Ticket.queueTypes
        state = this@Ticket.state.toProto()
        matchId = this@Ticket.matchId ?: ""
        if (this@Ticket.assignment != null) {
            assignment = this@Ticket.assignment.toProto()
        }
        if (this@Ticket.countdownEndTime != null) {
            countdownEndTime = timestamp {
                seconds = this@Ticket.countdownEndTime.epochSecond
                nanos = this@Ticket.countdownEndTime.nano
            }
        }
        createdAt = timestamp {
            seconds = this@Ticket.createdAt.epochSecond
            nanos = this@Ticket.createdAt.nano
        }
        properties.putAll(this@Ticket.properties)
        failures.putAll(this@Ticket.failures)
    }

    companion object {
        fun fromProto(proto: TicketProto): Ticket = Ticket(
            id = proto.id,
            playerIds = proto.playerIdsList.map(UUID::fromString),
            queueTypes = proto.queueTypesList.toList(),
            state = TicketState.fromProto(proto.state),
            createdAt = Instant.ofEpochSecond(proto.createdAt.seconds, proto.createdAt.nanos.toLong()),
            matchId = proto.matchId.ifEmpty { null },
            assignment = if (proto.hasAssignment()) Assignment.fromProto(proto.assignment) else null,
            countdownEndTime = if (proto.hasCountdownEndTime()) {
                Instant.ofEpochSecond(proto.countdownEndTime.seconds, proto.countdownEndTime.nanos.toLong())
            } else {
                null
            },
            properties = proto.propertiesMap.toMap(),
            failures = proto.failuresMap.toMap(),
        )
    }

}

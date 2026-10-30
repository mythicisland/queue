package net.mythicisland.queue.shared.match

import build.buf.gen.mythicisland.queue.v1.match
import com.google.protobuf.timestamp
import net.mythicisland.queue.shared.ticket.Ticket
import java.time.Instant
import build.buf.gen.mythicisland.queue.v1.Match as MatchProto

data class Match(
    val id: String,
    val queueType: String,
    val tickets: List<Ticket>,
    val teams: List<Team>,
    val state: MatchState,
    val createdAt: Instant,
    val assignment: Assignment? = null,
    val countdownEndTime: Instant? = null,
) {

    fun without(tickets: List<Ticket>): Match {
        val ids = tickets.map { it.id }.toSet()
        val players = tickets.flatMap { it.playerIds }.toSet()
        return copy(
            tickets = this.tickets.filter { it.id !in ids },
            teams = teams.map { team -> Team(team.playerIds - players) },
        )
    }

    fun toProto(): MatchProto = match {
        id = this@Match.id
        queueType = this@Match.queueType
        tickets += this@Match.tickets.map { it.toProto() }
        teams += this@Match.teams.map { it.toProto() }
        state = this@Match.state.toProto()
        if (this@Match.assignment != null) {
            assignment = this@Match.assignment.toProto()
        }
        if (this@Match.countdownEndTime != null) {
            countdownEndTime = timestamp {
                seconds = this@Match.countdownEndTime.epochSecond
                nanos = this@Match.countdownEndTime.nano
            }
        }
        createdAt = timestamp {
            seconds = this@Match.createdAt.epochSecond
            nanos = this@Match.createdAt.nano
        }
    }

    companion object {
        fun fromProto(proto: MatchProto): Match = Match(
            id = proto.id,
            queueType = proto.queueType,
            tickets = proto.ticketsList.map(Ticket::fromProto),
            teams = proto.teamsList.map(Team::fromProto),
            state = MatchState.fromProto(proto.state),
            createdAt = Instant.ofEpochSecond(proto.createdAt.seconds, proto.createdAt.nanos.toLong()),
            assignment = if (proto.hasAssignment()) Assignment.fromProto(proto.assignment) else null,
            countdownEndTime = if (proto.hasCountdownEndTime()) {
                Instant.ofEpochSecond(proto.countdownEndTime.seconds, proto.countdownEndTime.nanos.toLong())
            } else {
                null
            },
        )

    }

}

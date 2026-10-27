package net.mythicisland.queue.api.internal

import com.google.protobuf.Timestamp
import net.mythicisland.queue.api.match.Assignment
import net.mythicisland.queue.api.match.Match
import net.mythicisland.queue.api.match.MatchReleaseReason
import net.mythicisland.queue.api.match.MatchState
import net.mythicisland.queue.api.match.Team
import net.mythicisland.queue.api.stats.QueueStats
import net.mythicisland.queue.api.ticket.Ticket
import net.mythicisland.queue.api.ticket.TicketDeleteReason
import net.mythicisland.queue.api.ticket.TicketState
import net.mythicisland.queue.api.type.MatchConfig
import net.mythicisland.queue.api.type.PartyConfig
import net.mythicisland.queue.api.type.QueueType
import net.mythicisland.queue.api.type.ServerConfig
import net.mythicisland.queue.api.type.SharedMode
import net.mythicisland.queue.api.type.TeamConfig
import java.time.Duration
import java.time.Instant
import java.util.UUID
import build.buf.gen.mythicisland.queue.v1.Assignment as ProtoAssignment
import build.buf.gen.mythicisland.queue.v1.Match as ProtoMatch
import build.buf.gen.mythicisland.queue.v1.MatchConfig as ProtoMatchConfig
import build.buf.gen.mythicisland.queue.v1.MatchReleaseReason as ProtoMatchReleaseReason
import build.buf.gen.mythicisland.queue.v1.MatchState as ProtoMatchState
import build.buf.gen.mythicisland.queue.v1.QueueStats as ProtoQueueStats
import build.buf.gen.mythicisland.queue.v1.QueueType as ProtoQueueType
import build.buf.gen.mythicisland.queue.v1.ServerConfig as ProtoServerConfig
import build.buf.gen.mythicisland.queue.v1.SharedMode as ProtoSharedMode
import build.buf.gen.mythicisland.queue.v1.Team as ProtoTeam
import build.buf.gen.mythicisland.queue.v1.Ticket as ProtoTicket
import build.buf.gen.mythicisland.queue.v1.TicketDeleteReason as ProtoTicketDeleteReason
import build.buf.gen.mythicisland.queue.v1.TicketState as ProtoTicketState
import com.google.protobuf.Duration as ProtoDuration

internal object ProtoUtil {

    fun toTicket(proto: ProtoTicket): Ticket = Ticket(
        id = UUID.fromString(proto.id),
        playerIds = toUuids(proto.playerIdsList),
        queueTypes = proto.queueTypesList.toList(),
        state = toTicketState(proto.state),
        createdAt = toInstant(proto.createdAt),
        matchId = if (proto.matchId.isEmpty()) null else UUID.fromString(proto.matchId),
        assignment = if (proto.hasAssignment()) toAssignment(proto.assignment) else null,
        countdownEndsAt = if (proto.hasCountdownEndTime()) toInstant(proto.countdownEndTime) else null,
        properties = proto.propertiesMap.toMap(),
    )

    fun toMatch(proto: ProtoMatch): Match = Match(
        id = UUID.fromString(proto.id),
        queueType = proto.queueType,
        tickets = proto.ticketsList.map(::toTicket),
        teams = proto.teamsList.map(::toTeam),
        state = toMatchState(proto.state),
        createdAt = toInstant(proto.createdAt),
        assignment = if (proto.hasAssignment()) toAssignment(proto.assignment) else null,
        countdownEndsAt = if (proto.hasCountdownEndTime()) toInstant(proto.countdownEndTime) else null,
    )

    fun toTeam(proto: ProtoTeam): Team = Team(toUuids(proto.playerIdsList))

    fun toAssignment(proto: ProtoAssignment): Assignment = Assignment(proto.serverId, proto.serverName)

    fun toTicketState(state: ProtoTicketState): TicketState = when (state) {
        ProtoTicketState.TICKET_STATE_SEARCHING -> TicketState.SEARCHING
        ProtoTicketState.TICKET_STATE_MATCHED -> TicketState.MATCHED
        ProtoTicketState.TICKET_STATE_ASSIGNED -> TicketState.ASSIGNED
        else -> throw IllegalArgumentException("Unknown ticket state: $state")
    }

    fun toMatchState(state: ProtoMatchState): MatchState = when (state) {
        ProtoMatchState.MATCH_STATE_ALLOCATING -> MatchState.ALLOCATING
        ProtoMatchState.MATCH_STATE_OFFERED -> MatchState.OFFERED
        ProtoMatchState.MATCH_STATE_COUNTDOWN -> MatchState.COUNTDOWN
        ProtoMatchState.MATCH_STATE_TRANSFERRING -> MatchState.TRANSFERRING
        ProtoMatchState.MATCH_STATE_COMPLETED -> MatchState.COMPLETED
        ProtoMatchState.MATCH_STATE_FAILED -> MatchState.FAILED
        ProtoMatchState.MATCH_STATE_CANCELLED -> MatchState.CANCELLED
        else -> throw IllegalArgumentException("Unknown match state: $state")
    }

    fun toDeleteReason(reason: ProtoTicketDeleteReason): TicketDeleteReason = when (reason) {
        ProtoTicketDeleteReason.TICKET_DELETE_REASON_CANCELLED -> TicketDeleteReason.CANCELLED
        ProtoTicketDeleteReason.TICKET_DELETE_REASON_TRANSFERRED -> TicketDeleteReason.TRANSFERRED
        ProtoTicketDeleteReason.TICKET_DELETE_REASON_DISCONNECTED -> TicketDeleteReason.DISCONNECTED
        else -> throw IllegalArgumentException("Unknown ticket delete reason: $reason")
    }

    fun toReleaseReason(reason: ProtoMatchReleaseReason): MatchReleaseReason = when (reason) {
        ProtoMatchReleaseReason.MATCH_RELEASE_REASON_RELEASED -> MatchReleaseReason.RELEASED
        ProtoMatchReleaseReason.MATCH_RELEASE_REASON_SERVER_STOPPED -> MatchReleaseReason.SERVER_STOPPED
        else -> throw IllegalArgumentException("Unknown match release reason: $reason")
    }

    fun toQueueType(proto: ProtoQueueType): QueueType = QueueType(
        name = proto.name,
        server = toServerConfig(proto.server),
        match = toMatchConfig(proto.match),
        properties = proto.propertiesMap.toMap(),
    )

    fun toServerConfig(proto: ProtoServerConfig): ServerConfig = ServerConfig(
        group = proto.group,
        allocationTimeout = toDuration(proto.allocationTimeout),
        shared = if (proto.hasShared()) toSharedMode(proto.shared) else null,
    )

    fun toSharedMode(proto: ProtoSharedMode): SharedMode = SharedMode(
        acceptTimeout = toDuration(proto.acceptTimeout),
        capacity = if (proto.hasCapacity()) proto.capacity else null,
    )

    fun toMatchConfig(proto: ProtoMatchConfig): MatchConfig = MatchConfig(
        minPlayers = proto.minPlayers,
        maxPlayers = proto.maxPlayers,
        fillTime = toDuration(proto.fillTime),
        countdown = toDuration(proto.countdown),
        teams = if (proto.hasTeams()) TeamConfig(proto.teams.count, proto.teams.size) else null,
        parties = PartyConfig(proto.parties.maxSize),
    )

    fun toQueueStats(proto: ProtoQueueStats): QueueStats = QueueStats(
        queueType = proto.queueType,
        searchingTickets = proto.searchingTickets,
        searchingPlayers = proto.searchingPlayers,
        activeMatches = proto.activeMatches,
    )

    fun toUuids(ids: List<String>): List<UUID> = ids.map(UUID::fromString)

    fun toDuration(duration: ProtoDuration): Duration = Duration.ofSeconds(duration.seconds, duration.nanos.toLong())

    fun toInstant(timestamp: Timestamp): Instant = Instant.ofEpochSecond(timestamp.seconds, timestamp.nanos.toLong())

}

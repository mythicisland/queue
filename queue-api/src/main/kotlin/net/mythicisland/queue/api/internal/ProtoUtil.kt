package net.mythicisland.queue.api.internal

import com.google.protobuf.Timestamp
import io.grpc.Status
import io.grpc.StatusException
import net.mythicisland.queue.api.QueueException
import net.mythicisland.queue.api.match.Assignment
import net.mythicisland.queue.api.match.Match
import net.mythicisland.queue.api.match.MatchReleaseReason
import net.mythicisland.queue.api.match.MatchState
import net.mythicisland.queue.api.match.Team
import net.mythicisland.queue.api.stats.QueueStats
import net.mythicisland.queue.api.ticket.Ticket
import net.mythicisland.queue.api.ticket.TicketDeleteReason
import net.mythicisland.queue.api.ticket.TicketState
import net.mythicisland.queue.api.type.EqualRule
import net.mythicisland.queue.api.type.MatchConfig
import net.mythicisland.queue.api.type.MatchFilter
import net.mythicisland.queue.api.type.MatchQuality
import net.mythicisland.queue.api.type.MatchRule
import net.mythicisland.queue.api.type.NumberFilter
import net.mythicisland.queue.api.type.PartyConfig
import net.mythicisland.queue.api.type.PresenceFilter
import net.mythicisland.queue.api.type.QueueType
import net.mythicisland.queue.api.type.RangeRule
import net.mythicisland.queue.api.type.ServerConfig
import net.mythicisland.queue.api.type.SharedMode
import net.mythicisland.queue.api.type.SpreadLimit
import net.mythicisland.queue.api.type.TeamConfig
import net.mythicisland.queue.api.type.ValuesFilter
import net.mythicisland.queue.api.type.Window
import java.time.Duration
import java.time.Instant
import java.util.UUID
import build.buf.gen.mythicisland.queue.v1.Assignment as ProtoAssignment
import build.buf.gen.mythicisland.queue.v1.EqualRule as ProtoEqualRule
import build.buf.gen.mythicisland.queue.v1.ExclusiveMode as ProtoExclusiveMode
import build.buf.gen.mythicisland.queue.v1.Match as ProtoMatch
import build.buf.gen.mythicisland.queue.v1.MatchConfig as ProtoMatchConfig
import build.buf.gen.mythicisland.queue.v1.MatchFilter as ProtoMatchFilter
import build.buf.gen.mythicisland.queue.v1.MatchQuality as ProtoMatchQuality
import build.buf.gen.mythicisland.queue.v1.MatchRule as ProtoMatchRule
import build.buf.gen.mythicisland.queue.v1.MatchReleaseReason as ProtoMatchReleaseReason
import build.buf.gen.mythicisland.queue.v1.MatchState as ProtoMatchState
import build.buf.gen.mythicisland.queue.v1.NumberFilter as ProtoNumberFilter
import build.buf.gen.mythicisland.queue.v1.PartyConfig as ProtoPartyConfig
import build.buf.gen.mythicisland.queue.v1.PresenceFilter as ProtoPresenceFilter
import build.buf.gen.mythicisland.queue.v1.QueueStats as ProtoQueueStats
import build.buf.gen.mythicisland.queue.v1.QueueType as ProtoQueueType
import build.buf.gen.mythicisland.queue.v1.RangeRule as ProtoRangeRule
import build.buf.gen.mythicisland.queue.v1.ServerConfig as ProtoServerConfig
import build.buf.gen.mythicisland.queue.v1.SharedMode as ProtoSharedMode
import build.buf.gen.mythicisland.queue.v1.SpreadLimit as ProtoSpreadLimit
import build.buf.gen.mythicisland.queue.v1.Team as ProtoTeam
import build.buf.gen.mythicisland.queue.v1.TeamConfig as ProtoTeamConfig
import build.buf.gen.mythicisland.queue.v1.Ticket as ProtoTicket
import build.buf.gen.mythicisland.queue.v1.TicketDeleteReason as ProtoTicketDeleteReason
import build.buf.gen.mythicisland.queue.v1.TicketState as ProtoTicketState
import build.buf.gen.mythicisland.queue.v1.ValuesFilter as ProtoValuesFilter
import build.buf.gen.mythicisland.queue.v1.Window as ProtoWindow
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
        failures = proto.failuresMap.toMap(),
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
        ProtoTicketDeleteReason.TICKET_DELETE_REASON_EXPIRED -> TicketDeleteReason.EXPIRED
        ProtoTicketDeleteReason.TICKET_DELETE_REASON_QUEUE_TYPE_INACTIVE -> TicketDeleteReason.QUEUE_TYPE_INACTIVE
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
        active = if (proto.hasActive()) proto.active else true,
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
        teams = if (proto.hasTeams()) toTeamConfig(proto.teams) else null,
        parties = PartyConfig(proto.parties.maxSize),
        rules = proto.rulesList.mapNotNull(::toMatchRule),
        maxWait = if (proto.hasMaxWait()) toDuration(proto.maxWait) else null,
        maxFailures = if (proto.hasMaxFailures()) proto.maxFailures else null,
        filters = proto.filtersList.mapNotNull(::toMatchFilter),
        quality = toMatchQuality(proto.quality),
    )

    fun toMatchFilter(proto: ProtoMatchFilter): MatchFilter? = when (proto.conditionCase) {
        ProtoMatchFilter.ConditionCase.VALUES -> ValuesFilter(proto.key, proto.values.valuesList.toList())
        ProtoMatchFilter.ConditionCase.NUMBER -> NumberFilter(
            key = proto.key,
            min = if (proto.number.hasMin()) proto.number.min else null,
            max = if (proto.number.hasMax()) proto.number.max else null,
        )
        ProtoMatchFilter.ConditionCase.PRESENCE -> PresenceFilter(proto.key, proto.presence.present)
        else -> null
    }

    fun toMatchQuality(proto: ProtoMatchQuality): MatchQuality = MatchQuality(
        spread = proto.spreadList.map { SpreadLimit(it.key, toWindow(it.window)) },
        teamBalance = if (proto.hasTeamBalance()) toWindow(proto.teamBalance) else null,
    )

    fun toWindow(proto: ProtoWindow): Window = Window(
        start = proto.start,
        widen = proto.widen,
        every = if (proto.hasEvery()) toDuration(proto.every) else null,
        max = if (proto.hasMax()) proto.max else null,
    )

    fun toTeamConfig(proto: ProtoTeamConfig): TeamConfig = TeamConfig(
        count = proto.count,
        size = proto.size,
        balanceBy = if (proto.hasBalanceBy()) proto.balanceBy else null,
    )

    fun toMatchRule(proto: ProtoMatchRule): MatchRule? = when (proto.ruleCase) {
        ProtoMatchRule.RuleCase.EQUAL -> EqualRule(
            key = proto.key,
            relaxAfter = if (proto.equal.hasRelaxAfter()) toDuration(proto.equal.relaxAfter) else null,
        )
        ProtoMatchRule.RuleCase.RANGE -> RangeRule(
            key = proto.key,
            start = proto.range.start,
            widen = proto.range.widen,
            every = if (proto.range.hasEvery()) toDuration(proto.range.every) else null,
            max = if (proto.range.hasMax()) proto.range.max else null,
        )
        else -> null
    }

    fun fromQueueType(type: QueueType): ProtoQueueType = ProtoQueueType.newBuilder()
        .setName(type.name)
        .setServer(fromServerConfig(type.server))
        .setMatch(fromMatchConfig(type.match))
        .putAllProperties(type.properties)
        .setActive(type.active)
        .build()

    fun fromServerConfig(config: ServerConfig): ProtoServerConfig {
        val builder = ProtoServerConfig.newBuilder()
            .setGroup(config.group)
            .setAllocationTimeout(fromDuration(config.allocationTimeout))
        val shared = config.shared
        if (shared == null) {
            builder.setExclusive(ProtoExclusiveMode.getDefaultInstance())
        } else {
            builder.setShared(fromSharedMode(shared))
        }
        return builder.build()
    }

    fun fromSharedMode(mode: SharedMode): ProtoSharedMode {
        val builder = ProtoSharedMode.newBuilder()
            .setAcceptTimeout(fromDuration(mode.acceptTimeout))
        val capacity = mode.capacity
        if (capacity != null) {
            builder.setCapacity(capacity)
        }
        return builder.build()
    }

    fun fromMatchConfig(config: MatchConfig): ProtoMatchConfig {
        val builder = ProtoMatchConfig.newBuilder()
            .setMinPlayers(config.minPlayers)
            .setMaxPlayers(config.maxPlayers)
            .setFillTime(fromDuration(config.fillTime))
            .setCountdown(fromDuration(config.countdown))
            .setParties(ProtoPartyConfig.newBuilder().setMaxSize(config.parties.maxSize))
            .addAllRules(config.rules.map(::fromMatchRule))
            .addAllFilters(config.filters.map(::fromMatchFilter))
            .setQuality(fromMatchQuality(config.quality))
        val teams = config.teams
        if (teams != null) {
            builder.setTeams(fromTeamConfig(teams))
        }
        val maxWait = config.maxWait
        if (maxWait != null) {
            builder.setMaxWait(fromDuration(maxWait))
        }
        val maxFailures = config.maxFailures
        if (maxFailures != null) {
            builder.setMaxFailures(maxFailures)
        }
        return builder.build()
    }

    fun fromTeamConfig(config: TeamConfig): ProtoTeamConfig {
        val builder = ProtoTeamConfig.newBuilder()
            .setCount(config.count)
            .setSize(config.size)
        val balanceBy = config.balanceBy
        if (balanceBy != null) {
            builder.setBalanceBy(balanceBy)
        }
        return builder.build()
    }

    fun fromMatchRule(rule: MatchRule): ProtoMatchRule {
        val builder = ProtoMatchRule.newBuilder().setKey(rule.key)
        when (rule) {
            is EqualRule -> builder.setEqual(fromEqualRule(rule))
            is RangeRule -> builder.setRange(fromRangeRule(rule))
        }
        return builder.build()
    }

    fun fromEqualRule(rule: EqualRule): ProtoEqualRule {
        val builder = ProtoEqualRule.newBuilder()
        val relaxAfter = rule.relaxAfter
        if (relaxAfter != null) {
            builder.setRelaxAfter(fromDuration(relaxAfter))
        }
        return builder.build()
    }

    fun fromRangeRule(rule: RangeRule): ProtoRangeRule {
        val builder = ProtoRangeRule.newBuilder()
            .setStart(rule.start)
            .setWiden(rule.widen)
        val every = rule.every
        if (every != null) {
            builder.setEvery(fromDuration(every))
        }
        val max = rule.max
        if (max != null) {
            builder.setMax(max)
        }
        return builder.build()
    }

    fun fromMatchFilter(filter: MatchFilter): ProtoMatchFilter {
        val builder = ProtoMatchFilter.newBuilder().setKey(filter.key)
        when (filter) {
            is ValuesFilter -> builder.setValues(ProtoValuesFilter.newBuilder().addAllValues(filter.values))
            is NumberFilter -> builder.setNumber(fromNumberFilter(filter))
            is PresenceFilter -> builder.setPresence(ProtoPresenceFilter.newBuilder().setPresent(filter.present))
        }
        return builder.build()
    }

    fun fromNumberFilter(filter: NumberFilter): ProtoNumberFilter {
        val builder = ProtoNumberFilter.newBuilder()
        val min = filter.min
        if (min != null) {
            builder.setMin(min)
        }
        val max = filter.max
        if (max != null) {
            builder.setMax(max)
        }
        return builder.build()
    }

    fun fromMatchQuality(quality: MatchQuality): ProtoMatchQuality {
        val builder = ProtoMatchQuality.newBuilder()
            .addAllSpread(quality.spread.map(::fromSpreadLimit))
        val teamBalance = quality.teamBalance
        if (teamBalance != null) {
            builder.setTeamBalance(fromWindow(teamBalance))
        }
        return builder.build()
    }

    fun fromSpreadLimit(limit: SpreadLimit): ProtoSpreadLimit = ProtoSpreadLimit.newBuilder()
        .setKey(limit.key)
        .setWindow(fromWindow(limit.window))
        .build()

    fun fromWindow(window: Window): ProtoWindow {
        val builder = ProtoWindow.newBuilder()
            .setStart(window.start)
            .setWiden(window.widen)
        val every = window.every
        if (every != null) {
            builder.setEvery(fromDuration(every))
        }
        val max = window.max
        if (max != null) {
            builder.setMax(max)
        }
        return builder.build()
    }

    fun fromDuration(duration: Duration): ProtoDuration = ProtoDuration.newBuilder()
        .setSeconds(duration.seconds)
        .setNanos(duration.nano)
        .build()

    fun toQueueStats(proto: ProtoQueueStats): QueueStats = QueueStats(
        queueType = proto.queueType,
        searchingTickets = proto.searchingTickets,
        searchingPlayers = proto.searchingPlayers,
        activeMatches = proto.activeMatches,
    )

    fun toUuids(ids: List<String>): List<UUID> = ids.map(UUID::fromString)

    fun toDuration(duration: ProtoDuration): Duration = Duration.ofSeconds(duration.seconds, duration.nanos.toLong())

    fun toInstant(timestamp: Timestamp): Instant = Instant.ofEpochSecond(timestamp.seconds, timestamp.nanos.toLong())

    fun toQueueException(e: StatusException): QueueException {
        val code = when (e.status.code) {
            Status.Code.NOT_FOUND -> QueueException.Code.NOT_FOUND
            Status.Code.ALREADY_EXISTS -> QueueException.Code.ALREADY_EXISTS
            Status.Code.INVALID_ARGUMENT -> QueueException.Code.INVALID_ARGUMENT
            Status.Code.FAILED_PRECONDITION -> QueueException.Code.FAILED_PRECONDITION
            Status.Code.PERMISSION_DENIED -> QueueException.Code.PERMISSION_DENIED
            Status.Code.UNAUTHENTICATED -> QueueException.Code.UNAUTHENTICATED
            Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED -> QueueException.Code.UNAVAILABLE
            else -> QueueException.Code.UNKNOWN
        }
        return QueueException(code, e.status.description ?: e.status.code.name, e)
    }

}

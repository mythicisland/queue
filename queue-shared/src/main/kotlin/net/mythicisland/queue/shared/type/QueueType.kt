package net.mythicisland.queue.shared.type

import build.buf.gen.mythicisland.queue.v1.equalRule
import build.buf.gen.mythicisland.queue.v1.exclusiveMode
import build.buf.gen.mythicisland.queue.v1.matchConfig
import build.buf.gen.mythicisland.queue.v1.matchFilter
import build.buf.gen.mythicisland.queue.v1.matchQuality
import build.buf.gen.mythicisland.queue.v1.matchRule
import build.buf.gen.mythicisland.queue.v1.numberFilter
import build.buf.gen.mythicisland.queue.v1.partyConfig
import build.buf.gen.mythicisland.queue.v1.presenceFilter
import build.buf.gen.mythicisland.queue.v1.queueType
import build.buf.gen.mythicisland.queue.v1.rangeRule
import build.buf.gen.mythicisland.queue.v1.serverConfig
import build.buf.gen.mythicisland.queue.v1.sharedMode
import build.buf.gen.mythicisland.queue.v1.spreadLimit
import build.buf.gen.mythicisland.queue.v1.teamConfig
import build.buf.gen.mythicisland.queue.v1.valuesFilter
import build.buf.gen.mythicisland.queue.v1.window
import com.google.protobuf.duration
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.time.Duration
import build.buf.gen.mythicisland.queue.v1.EqualRule as EqualRuleProto
import build.buf.gen.mythicisland.queue.v1.MatchConfig as MatchConfigProto
import build.buf.gen.mythicisland.queue.v1.MatchFilter as MatchFilterProto
import build.buf.gen.mythicisland.queue.v1.MatchQuality as MatchQualityProto
import build.buf.gen.mythicisland.queue.v1.NumberFilter as NumberFilterProto
import build.buf.gen.mythicisland.queue.v1.MatchRule as MatchRuleProto
import build.buf.gen.mythicisland.queue.v1.QueueType as QueueTypeProto
import build.buf.gen.mythicisland.queue.v1.RangeRule as RangeRuleProto
import build.buf.gen.mythicisland.queue.v1.ServerConfig as ServerConfigProto
import build.buf.gen.mythicisland.queue.v1.SharedMode as SharedModeProto
import build.buf.gen.mythicisland.queue.v1.SpreadLimit as SpreadLimitProto
import build.buf.gen.mythicisland.queue.v1.TeamConfig as TeamConfigProto
import build.buf.gen.mythicisland.queue.v1.Window as WindowProto
import com.google.protobuf.Duration as DurationProto

@ConfigSerializable
data class QueueType(
    val name: String = "",
    val server: ServerConfig = ServerConfig(),
    val match: MatchConfig = MatchConfig(),
    val properties: Map<String, String> = emptyMap(),
) {

    fun toProto(): QueueTypeProto = queueType {
        name = this@QueueType.name
        server = this@QueueType.server.toProto()
        match = this@QueueType.match.toProto()
        properties.putAll(this@QueueType.properties)
    }

    companion object {
        fun fromProto(proto: QueueTypeProto): QueueType = QueueType(
            name = proto.name,
            server = ServerConfig.fromProto(proto.server),
            match = MatchConfig.fromProto(proto.match),
            properties = proto.propertiesMap.toMap(),
        )
    }

}

@ConfigSerializable
data class ServerConfig(
    val group: String = "",
    val allocationTimeout: Duration = Duration.ofSeconds(60),
    val shared: SharedMode? = null,
) {

    fun toProto(): ServerConfigProto = serverConfig {
        group = this@ServerConfig.group
        allocationTimeout = this@ServerConfig.allocationTimeout.toProto()
        if (this@ServerConfig.shared == null) {
            exclusive = exclusiveMode {}
        } else {
            shared = this@ServerConfig.shared.toProto()
        }
    }

    companion object {
        fun fromProto(proto: ServerConfigProto): ServerConfig = ServerConfig(
            group = proto.group,
            allocationTimeout = proto.allocationTimeout.toJavaDuration(),
            shared = if (proto.hasShared()) SharedMode.fromProto(proto.shared) else null,
        )
    }

}

@ConfigSerializable
data class SharedMode(
    val acceptTimeout: Duration = Duration.ofSeconds(10),
    val capacity: Int? = null,
) {

    fun toProto(): SharedModeProto = sharedMode {
        acceptTimeout = this@SharedMode.acceptTimeout.toProto()
        if (this@SharedMode.capacity != null) {
            capacity = this@SharedMode.capacity
        }
    }

    companion object {
        fun fromProto(proto: SharedModeProto): SharedMode = SharedMode(
            acceptTimeout = proto.acceptTimeout.toJavaDuration(),
            capacity = if (proto.hasCapacity()) proto.capacity else null,
        )
    }

}

@ConfigSerializable
data class MatchConfig(
    val minPlayers: Int = 0,
    val maxPlayers: Int? = null,
    val fillTime: Duration = Duration.ZERO,
    val countdown: Duration = Duration.ZERO,
    val teams: TeamConfig? = null,
    val parties: PartyConfig = PartyConfig(),
    val rules: List<MatchRule> = emptyList(),
    val maxWait: Duration? = null,
    val maxFailures: Int? = null,
    val filters: List<MatchFilter> = emptyList(),
    val quality: MatchQuality = MatchQuality(),
) {

    val effectiveMaxPlayers: Int
        get() = if (teams != null) teams.count * teams.size else maxPlayers ?: 0

    val effectivePartySize: Int
        get() = parties.maxSize ?: (teams?.size ?: effectiveMaxPlayers)

    val numberProperties: Set<String>
        get() = rules.filter { it.type == MatchRuleType.RANGE }.map { it.key }.toSet() +
            quality.spread.map { it.key } +
            listOfNotNull(teams?.balanceBy)

    fun toProto(): MatchConfigProto = matchConfig {
        minPlayers = this@MatchConfig.minPlayers
        maxPlayers = this@MatchConfig.effectiveMaxPlayers
        fillTime = this@MatchConfig.fillTime.toProto()
        countdown = this@MatchConfig.countdown.toProto()
        if (this@MatchConfig.teams != null) {
            teams = this@MatchConfig.teams.toProto()
        }
        parties = partyConfig { maxSize = this@MatchConfig.effectivePartySize }
        rules += this@MatchConfig.rules.map { it.toProto() }
        if (this@MatchConfig.maxWait != null) {
            maxWait = this@MatchConfig.maxWait.toProto()
        }
        if (this@MatchConfig.maxFailures != null) {
            maxFailures = this@MatchConfig.maxFailures
        }
        filters += this@MatchConfig.filters.map { it.toProto() }
        quality = this@MatchConfig.quality.toProto()
    }

    companion object {
        fun fromProto(proto: MatchConfigProto): MatchConfig = MatchConfig(
            minPlayers = proto.minPlayers,
            maxPlayers = if (proto.hasTeams()) null else proto.maxPlayers,
            fillTime = proto.fillTime.toJavaDuration(),
            countdown = proto.countdown.toJavaDuration(),
            teams = if (proto.hasTeams()) TeamConfig.fromProto(proto.teams) else null,
            parties = PartyConfig(proto.parties.maxSize),
            rules = proto.rulesList.map(MatchRule::fromProto),
            maxWait = if (proto.hasMaxWait()) proto.maxWait.toJavaDuration() else null,
            maxFailures = if (proto.hasMaxFailures()) proto.maxFailures else null,
            filters = proto.filtersList.map(MatchFilter::fromProto),
            quality = MatchQuality.fromProto(proto.quality),
        )
    }

}

@ConfigSerializable
data class MatchQuality(
    val spread: List<SpreadLimit> = emptyList(),
    val teamBalance: Window? = null,
) {

    fun hasLimits(): Boolean = spread.isNotEmpty() || teamBalance != null

    fun toProto(): MatchQualityProto = matchQuality {
        spread += this@MatchQuality.spread.map { it.toProto() }
        if (this@MatchQuality.teamBalance != null) {
            teamBalance = this@MatchQuality.teamBalance.toProto()
        }
    }

    companion object {
        fun fromProto(proto: MatchQualityProto): MatchQuality = MatchQuality(
            spread = proto.spreadList.map(SpreadLimit::fromProto),
            teamBalance = if (proto.hasTeamBalance()) Window.fromProto(proto.teamBalance) else null,
        )
    }

}

@ConfigSerializable
data class SpreadLimit(
    val key: String = "",
    val start: Double? = null,
    val widen: Double? = null,
    val every: Duration? = null,
    val max: Double? = null,
) {

    val window: Window
        get() = Window(start, widen, every, max)

    fun toProto(): SpreadLimitProto = spreadLimit {
        key = this@SpreadLimit.key
        window = this@SpreadLimit.window.toProto()
    }

    companion object {
        fun fromProto(proto: SpreadLimitProto): SpreadLimit {
            val window = Window.fromProto(proto.window)
            return SpreadLimit(proto.key, window.start, window.widen, window.every, window.max)
        }
    }

}

@ConfigSerializable
data class MatchFilter(
    val key: String = "",
    val values: List<String>? = null,
    val min: Double? = null,
    val max: Double? = null,
    val present: Boolean? = null,
) {

    fun accepts(properties: Map<String, String>): Boolean {
        val value = properties[key]
        if (present != null) {
            return (value != null) == present
        }
        if (value == null) {
            return false
        }
        if (values != null) {
            return value in values
        }

        val number = value.toDoubleOrNull() ?: return false
        return number.isFinite() && (min == null || number >= min) && (max == null || number <= max)
    }

    fun describe(): String = when {
        present == true -> "$key to be set"
        present == false -> "$key not to be set"
        values != null -> "$key to be one of ${values.joinToString()}"
        min != null && max != null -> "$key to be a number from ${format(min)} to ${format(max)}"
        min != null -> "$key to be a number of at least ${format(min)}"
        max != null -> "$key to be a number of at most ${format(max)}"
        else -> "$key to be a number"
    }

    fun toProto(): MatchFilterProto = matchFilter {
        key = this@MatchFilter.key
        if (this@MatchFilter.present != null) {
            presence = presenceFilter { present = this@MatchFilter.present }
        } else if (this@MatchFilter.values != null) {
            values = valuesFilter { values += this@MatchFilter.values }
        } else {
            number = toNumberProto()
        }
    }

    private fun toNumberProto(): NumberFilterProto = numberFilter {
        if (this@MatchFilter.min != null) {
            min = this@MatchFilter.min
        }
        if (this@MatchFilter.max != null) {
            max = this@MatchFilter.max
        }
    }

    private fun format(number: Double): String =
        if (number % 1.0 == 0.0) number.toLong().toString() else number.toString()

    companion object {
        fun fromProto(proto: MatchFilterProto): MatchFilter = when (proto.conditionCase) {
            MatchFilterProto.ConditionCase.VALUES -> MatchFilter(key = proto.key, values = proto.values.valuesList.toList())
            MatchFilterProto.ConditionCase.NUMBER -> MatchFilter(
                key = proto.key,
                min = if (proto.number.hasMin()) proto.number.min else null,
                max = if (proto.number.hasMax()) proto.number.max else null,
            )
            MatchFilterProto.ConditionCase.PRESENCE -> MatchFilter(key = proto.key, present = proto.presence.present)
            else -> MatchFilter(key = proto.key)
        }
    }

}

enum class MatchRuleType {
    EQUAL,
    RANGE,
}

@ConfigSerializable
data class MatchRule(
    val type: MatchRuleType? = null,
    val key: String = "",
    val relaxAfter: Duration? = null,
    val start: Double? = null,
    val widen: Double? = null,
    val every: Duration? = null,
    val max: Double? = null,
) {

    val window: Window
        get() = Window(start, widen, every, max)

    fun toProto(): MatchRuleProto = matchRule {
        key = this@MatchRule.key
        if (this@MatchRule.type == MatchRuleType.EQUAL) {
            equal = toEqualProto()
        }
        if (this@MatchRule.type == MatchRuleType.RANGE) {
            range = toRangeProto()
        }
    }

    private fun toEqualProto(): EqualRuleProto = equalRule {
        if (this@MatchRule.relaxAfter != null) {
            relaxAfter = this@MatchRule.relaxAfter.toProto()
        }
    }

    private fun toRangeProto(): RangeRuleProto = rangeRule {
        start = this@MatchRule.start ?: 0.0
        widen = this@MatchRule.widen ?: 0.0
        if (this@MatchRule.every != null) {
            every = this@MatchRule.every.toProto()
        }
        if (this@MatchRule.max != null) {
            max = this@MatchRule.max
        }
    }

    companion object {
        fun fromProto(proto: MatchRuleProto): MatchRule = when (proto.ruleCase) {
            MatchRuleProto.RuleCase.EQUAL -> MatchRule(
                type = MatchRuleType.EQUAL,
                key = proto.key,
                relaxAfter = if (proto.equal.hasRelaxAfter()) proto.equal.relaxAfter.toJavaDuration() else null,
            )
            MatchRuleProto.RuleCase.RANGE -> MatchRule(
                type = MatchRuleType.RANGE,
                key = proto.key,
                start = proto.range.start,
                widen = proto.range.widen,
                every = if (proto.range.hasEvery()) proto.range.every.toJavaDuration() else null,
                max = if (proto.range.hasMax()) proto.range.max else null,
            )
            else -> MatchRule(key = proto.key)
        }
    }

}

@ConfigSerializable
data class Window(
    val start: Double? = null,
    val widen: Double? = null,
    val every: Duration? = null,
    val max: Double? = null,
) {

    fun at(waited: Duration): Double {
        val base = start ?: 0.0
        val step = widen ?: 0.0
        if (every == null || !every.isPositive || step == 0.0 || waited.isNegative) {
            return base
        }

        val grown = base + step * waited.dividedBy(every)
        if (max == null) {
            return grown
        }
        return minOf(grown, max)
    }

    fun toProto(): WindowProto = window {
        start = this@Window.start ?: 0.0
        widen = this@Window.widen ?: 0.0
        if (this@Window.every != null) {
            every = this@Window.every.toProto()
        }
        if (this@Window.max != null) {
            max = this@Window.max
        }
    }

    companion object {
        fun fromProto(proto: WindowProto): Window = Window(
            start = proto.start,
            widen = proto.widen,
            every = if (proto.hasEvery()) proto.every.toJavaDuration() else null,
            max = if (proto.hasMax()) proto.max else null,
        )
    }

}

@ConfigSerializable
data class TeamConfig(
    val count: Int = 0,
    val size: Int = 0,
    val balanceBy: String? = null,
) {

    fun toProto(): TeamConfigProto = teamConfig {
        count = this@TeamConfig.count
        size = this@TeamConfig.size
        if (this@TeamConfig.balanceBy != null) {
            balanceBy = this@TeamConfig.balanceBy
        }
    }

    companion object {
        fun fromProto(proto: TeamConfigProto): TeamConfig = TeamConfig(
            count = proto.count,
            size = proto.size,
            balanceBy = if (proto.hasBalanceBy()) proto.balanceBy else null,
        )
    }

}

@ConfigSerializable
data class PartyConfig(
    val maxSize: Int? = null,
)

private fun Duration.toProto(): DurationProto = duration {
    seconds = this@toProto.seconds
    nanos = this@toProto.nano
}

private fun DurationProto.toJavaDuration(): Duration = Duration.ofSeconds(seconds, nanos.toLong())

package net.mythicisland.queue.shared.type

import build.buf.gen.mythicisland.queue.v1.matchConfig
import build.buf.gen.mythicisland.queue.v1.partyConfig
import build.buf.gen.mythicisland.queue.v1.teamConfig
import net.mythicisland.queue.shared.toJavaDuration
import net.mythicisland.queue.shared.toProto
import java.time.Duration
import build.buf.gen.mythicisland.queue.v1.MatchConfig as MatchConfigProto
import build.buf.gen.mythicisland.queue.v1.TeamConfig as TeamConfigProto

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
            parties = PartyConfig(if (proto.parties.maxSize == 0) null else proto.parties.maxSize),
            rules = proto.rulesList.map(MatchRule::fromProto),
            maxWait = if (proto.hasMaxWait()) proto.maxWait.toJavaDuration() else null,
            maxFailures = if (proto.hasMaxFailures()) proto.maxFailures else null,
            filters = proto.filtersList.map(MatchFilter::fromProto),
            quality = MatchQuality.fromProto(proto.quality),
        )
    }

}

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

data class PartyConfig(
    val maxSize: Int? = null,
)

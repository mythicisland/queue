package net.mythicisland.queue.shared.type

import build.buf.gen.mythicisland.queue.v1.equalRule
import build.buf.gen.mythicisland.queue.v1.matchRule
import build.buf.gen.mythicisland.queue.v1.rangeRule
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.time.Duration
import build.buf.gen.mythicisland.queue.v1.EqualRule as EqualRuleProto
import build.buf.gen.mythicisland.queue.v1.MatchRule as MatchRuleProto
import build.buf.gen.mythicisland.queue.v1.RangeRule as RangeRuleProto

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

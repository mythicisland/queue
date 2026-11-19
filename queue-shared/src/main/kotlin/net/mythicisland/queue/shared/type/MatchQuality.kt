package net.mythicisland.queue.shared.type

import build.buf.gen.mythicisland.queue.v1.matchQuality
import build.buf.gen.mythicisland.queue.v1.spreadLimit
import java.time.Duration
import build.buf.gen.mythicisland.queue.v1.MatchQuality as MatchQualityProto
import build.buf.gen.mythicisland.queue.v1.SpreadLimit as SpreadLimitProto

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

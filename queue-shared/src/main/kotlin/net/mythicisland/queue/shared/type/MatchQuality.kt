package net.mythicisland.queue.shared.type

import build.buf.gen.mythicisland.queue.v1.matchQuality
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import build.buf.gen.mythicisland.queue.v1.MatchQuality as MatchQualityProto

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

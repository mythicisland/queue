package net.mythicisland.queue.shared.type

import build.buf.gen.mythicisland.queue.v1.teamConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import build.buf.gen.mythicisland.queue.v1.TeamConfig as TeamConfigProto

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

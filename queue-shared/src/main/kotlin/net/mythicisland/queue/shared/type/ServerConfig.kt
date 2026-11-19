package net.mythicisland.queue.shared.type

import build.buf.gen.mythicisland.queue.v1.exclusiveMode
import build.buf.gen.mythicisland.queue.v1.serverConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.time.Duration
import build.buf.gen.mythicisland.queue.v1.ServerConfig as ServerConfigProto

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

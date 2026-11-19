package net.mythicisland.queue.shared.type

import build.buf.gen.mythicisland.queue.v1.exclusiveMode
import build.buf.gen.mythicisland.queue.v1.serverConfig
import build.buf.gen.mythicisland.queue.v1.sharedMode
import net.mythicisland.queue.shared.toJavaDuration
import net.mythicisland.queue.shared.toProto
import java.time.Duration
import build.buf.gen.mythicisland.queue.v1.ServerConfig as ServerConfigProto
import build.buf.gen.mythicisland.queue.v1.SharedMode as SharedModeProto

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

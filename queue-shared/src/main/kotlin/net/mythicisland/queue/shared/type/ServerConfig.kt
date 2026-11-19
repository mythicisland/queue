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
    val allocationTimeout: Duration = DEFAULT_ALLOCATION_TIMEOUT,
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
        private val DEFAULT_ALLOCATION_TIMEOUT: Duration = Duration.ofSeconds(60)

        fun fromProto(proto: ServerConfigProto): ServerConfig = ServerConfig(
            group = proto.group,
            allocationTimeout = if (proto.hasAllocationTimeout()) proto.allocationTimeout.toJavaDuration() else DEFAULT_ALLOCATION_TIMEOUT,
            shared = if (proto.hasShared()) SharedMode.fromProto(proto.shared) else null,
        )
    }

}

data class SharedMode(
    val acceptTimeout: Duration = DEFAULT_ACCEPT_TIMEOUT,
    val capacity: Int? = null,
) {

    fun toProto(): SharedModeProto = sharedMode {
        acceptTimeout = this@SharedMode.acceptTimeout.toProto()
        if (this@SharedMode.capacity != null) {
            capacity = this@SharedMode.capacity
        }
    }

    companion object {
        private val DEFAULT_ACCEPT_TIMEOUT: Duration = Duration.ofSeconds(10)

        fun fromProto(proto: SharedModeProto): SharedMode = SharedMode(
            acceptTimeout = if (proto.hasAcceptTimeout()) proto.acceptTimeout.toJavaDuration() else DEFAULT_ACCEPT_TIMEOUT,
            capacity = if (proto.hasCapacity()) proto.capacity else null,
        )
    }

}

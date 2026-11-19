package net.mythicisland.queue.shared.type

import build.buf.gen.mythicisland.queue.v1.sharedMode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.time.Duration
import build.buf.gen.mythicisland.queue.v1.SharedMode as SharedModeProto

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

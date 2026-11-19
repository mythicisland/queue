package net.mythicisland.queue.shared.type

import build.buf.gen.mythicisland.queue.v1.spreadLimit
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.time.Duration
import build.buf.gen.mythicisland.queue.v1.SpreadLimit as SpreadLimitProto

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

package net.mythicisland.queue.shared.type

import build.buf.gen.mythicisland.queue.v1.window
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.time.Duration
import build.buf.gen.mythicisland.queue.v1.Window as WindowProto

@ConfigSerializable
data class Window(
    val start: Double? = null,
    val widen: Double? = null,
    val every: Duration? = null,
    val max: Double? = null,
) {

    fun at(waited: Duration): Double {
        val base = start ?: 0.0
        val step = widen ?: 0.0
        if (every == null || !every.isPositive || step == 0.0 || waited.isNegative) {
            return base
        }

        val grown = base + step * waited.dividedBy(every)
        if (max == null) {
            return grown
        }
        return minOf(grown, max)
    }

    fun toProto(): WindowProto = window {
        start = this@Window.start ?: 0.0
        widen = this@Window.widen ?: 0.0
        if (this@Window.every != null) {
            every = this@Window.every.toProto()
        }
        if (this@Window.max != null) {
            max = this@Window.max
        }
    }

    companion object {
        fun fromProto(proto: WindowProto): Window = Window(
            start = proto.start,
            widen = proto.widen,
            every = if (proto.hasEvery()) proto.every.toJavaDuration() else null,
            max = if (proto.hasMax()) proto.max else null,
        )
    }

}

package net.mythicisland.queue.shared.type

import com.google.protobuf.duration
import java.time.Duration
import com.google.protobuf.Duration as DurationProto

internal fun Duration.toProto(): DurationProto = duration {
    seconds = this@toProto.seconds
    nanos = this@toProto.nano
}

internal fun DurationProto.toJavaDuration(): Duration = Duration.ofSeconds(seconds, nanos.toLong())

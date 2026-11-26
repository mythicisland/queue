package net.mythicisland.queue.shared

import java.time.Duration
import com.google.protobuf.Duration as ProtobufDuration

fun Duration.toProto(): ProtobufDuration = ProtobufDuration.newBuilder()
    .setSeconds(seconds)
    .setNanos(nano)
    .build()

fun ProtobufDuration.toJavaDuration(): Duration = Duration.ofSeconds(seconds, nanos.toLong())

package net.mythicisland.queue.shared.protobuf

import com.google.protobuf.Duration
import com.google.protobuf.Timestamp
import java.time.Instant

/**
 * Converts an [Instant] into its protobuf representation.
 */
fun Instant.toTimestamp(): Timestamp {
    return Timestamp.newBuilder()
        .setSeconds(epochSecond)
        .setNanos(nano)
        .build()
}

/**
 * Converts a protobuf [Timestamp] back into an [Instant].
 */
fun Timestamp.toInstant(): Instant {
    return Instant.ofEpochSecond(seconds, nanos.toLong())
}

/**
 * Converts an amount of seconds into a protobuf [Duration].
 */
fun Long.toProtoDuration(): Duration {
    return Duration.newBuilder()
        .setSeconds(this)
        .build()
}

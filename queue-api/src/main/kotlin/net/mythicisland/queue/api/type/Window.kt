package net.mythicisland.queue.api.type

import java.time.Duration

/**
 * A limit that starts at [start] and grows by [widen] every [every], up to [max] if it is set.
 */
data class Window(
    val start: Double,
    val widen: Double = 0.0,
    val every: Duration? = null,
    val max: Double? = null,
)

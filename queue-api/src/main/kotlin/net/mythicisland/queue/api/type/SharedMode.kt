package net.mythicisland.queue.api.type

import java.time.Duration

/**
 * Settings for servers hosting several matches at once.
 *
 * A server has [acceptTimeout] to accept an offered match before it goes to the next server.
 * [capacity] is how many matches a server hosts at once if it doesn't set the `queue.capacity` property itself,
 * `null` if every server has to set it.
 */
data class SharedMode(
    val acceptTimeout: Duration = Duration.ofSeconds(10),
    val capacity: Int? = null,
)

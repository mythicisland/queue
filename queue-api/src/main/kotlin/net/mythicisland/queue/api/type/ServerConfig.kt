package net.mythicisland.queue.api.type

import java.time.Duration

/**
 * Where the matches of a queue type run.
 *
 * Matches get a server of their own if [shared] is `null`, otherwise they are offered to servers
 * that host several matches at once.
 */
data class ServerConfig(
    val group: String,
    val allocationTimeout: Duration,
    val shared: SharedMode?,
)

/**
 * Settings for servers hosting several matches at once.
 *
 * A server has [acceptTimeout] to accept an offered match before it goes to the next server.
 * [capacity] is how many matches a server hosts at once if it doesn't set the `queue.capacity` property itself,
 * `null` if every server has to set it.
 */
data class SharedMode(
    val acceptTimeout: Duration,
    val capacity: Int?,
)

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
 */
data class SharedMode(
    val acceptTimeout: Duration,
)

package net.mythicisland.queue.api.type

/**
 * A queue players can join, e.g. `bedwars-4x4`.
 *
 * [properties] are free-form values from the queue type config, the queue itself doesn't read them.
 */
data class QueueType(
    val name: String,
    val server: ServerConfig,
    val match: MatchConfig,
    val properties: Map<String, String>,
)

package net.mythicisland.queue.api.type

/**
 * A queue players can join, e.g. `bedwars-4x4`.
 *
 * [properties] are free-form values from the queue type config, the queue itself doesn't read them.
 * An inactive queue type takes no new tickets and starts no new matches, its searching tickets are deleted
 * with [net.mythicisland.queue.api.ticket.TicketDeleteReason.QUEUE_TYPE_INACTIVE]. Running matches keep going.
 */
data class QueueType(
    val name: String,
    val server: ServerConfig,
    val match: MatchConfig,
    val properties: Map<String, String> = emptyMap(),
    val active: Boolean = true,
)

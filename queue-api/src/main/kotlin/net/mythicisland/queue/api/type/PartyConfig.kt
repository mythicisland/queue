package net.mythicisland.queue.api.type

/**
 * Tickets have at most [maxSize] players.
 */
data class PartyConfig(
    val maxSize: Int,
)

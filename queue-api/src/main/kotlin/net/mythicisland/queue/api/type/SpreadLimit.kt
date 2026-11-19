package net.mythicisland.queue.api.type

/**
 * The highest minus the lowest numeric value of [key] in a match may be at most [window].
 *
 * Tickets in a queue type with this limit must have [key] set to a number,
 * otherwise creating or updating them fails with [net.mythicisland.queue.api.QueueException.Code.INVALID_ARGUMENT].
 */
data class SpreadLimit(
    val key: String,
    val window: Window,
)

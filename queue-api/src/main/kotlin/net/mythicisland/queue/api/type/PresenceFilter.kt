package net.mythicisland.queue.api.type

/**
 * [key] must be set if [present] is `true`, or must not be set if it is `false`.
 */
data class PresenceFilter(
    override val key: String,
    val present: Boolean,
) : MatchFilter

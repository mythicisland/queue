package net.mythicisland.queue.api.type

/**
 * The value of [key] must be one of [values].
 */
data class ValuesFilter(
    override val key: String,
    val values: List<String>,
) : MatchFilter

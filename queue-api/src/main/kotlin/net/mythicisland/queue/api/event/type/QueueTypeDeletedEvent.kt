package net.mythicisland.queue.api.event.type

import net.mythicisland.queue.api.type.QueueType

/**
 * Fired when a queue type was deleted.
 */
data class QueueTypeDeletedEvent(
    val queueType: QueueType,
)

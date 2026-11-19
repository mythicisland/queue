package net.mythicisland.queue.api.event.type

import net.mythicisland.queue.api.type.QueueType

/**
 * Fired when a queue type was changed, [previous] is the queue type before the change.
 */
data class QueueTypeUpdatedEvent(
    val queueType: QueueType,
    val previous: QueueType,
)

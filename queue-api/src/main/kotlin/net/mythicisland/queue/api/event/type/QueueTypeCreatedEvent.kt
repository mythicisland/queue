package net.mythicisland.queue.api.event.type

import net.mythicisland.queue.api.type.QueueType

/**
 * Fired when a queue type was created.
 */
data class QueueTypeCreatedEvent(
    val queueType: QueueType,
)

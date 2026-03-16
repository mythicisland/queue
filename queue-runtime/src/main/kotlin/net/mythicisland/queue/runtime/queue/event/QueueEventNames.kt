package net.mythicisland.queue.runtime.queue.event

object QueueEventNames {

    private const val PREFIX = "queue.event."

    const val ENQUEUE = PREFIX + "enqueue"

    const val DEQUEUE = PREFIX + "dequeue"

    private const val QUEUE_PREFIX = PREFIX + "queue."

    const val QUEUE_CREATED = QUEUE_PREFIX + "created"

    const val QUEUE_UPDATED = QUEUE_PREFIX + "updated"

    const val QUEUE_DELETED = QUEUE_PREFIX + "deleted"

    const val QUEUE_TRANSFER = QUEUE_PREFIX + "transfer"

    const val QUEUE_STATUS_UPDATED = QUEUE_PREFIX + "status.updated"

    const val QUEUE_SERVER_ASSIGNED = QUEUE_PREFIX + "server.assigned"

}
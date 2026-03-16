package net.mythicisland.queue.api.internal.event;

/**
 * NATS subject names for queue events.
 *
 * <p>These must match the subjects used by the runtime's
 * {@code QueueEventNames} to ensure correct message routing.</p>
 */
final class QueueEventSubjects {

    private QueueEventSubjects() {
    }

    private static final String PREFIX = "queue.event.";

    static final String ENQUEUE = PREFIX + "enqueue";
    static final String DEQUEUE = PREFIX + "dequeue";

    private static final String QUEUE_PREFIX = PREFIX + "queue.";

    static final String QUEUE_CREATED = QUEUE_PREFIX + "created";
    static final String QUEUE_UPDATED = QUEUE_PREFIX + "updated";
    static final String QUEUE_DELETED = QUEUE_PREFIX + "deleted";
    static final String QUEUE_TRANSFER = QUEUE_PREFIX + "transfer";
    static final String QUEUE_STATUS_UPDATED = QUEUE_PREFIX + "status.updated";
    static final String QUEUE_SERVER_ASSIGNED = QUEUE_PREFIX + "server.assigned";
}

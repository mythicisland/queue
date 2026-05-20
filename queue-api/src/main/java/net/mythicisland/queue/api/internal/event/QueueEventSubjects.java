package net.mythicisland.queue.api.internal.event;

public final class QueueEventSubjects {

    private QueueEventSubjects() {
    }

    private static final String PREFIX = "queue.event.";

    public static final String ENQUEUE = PREFIX + "enqueue";
    public static final String DEQUEUE = PREFIX + "dequeue";

    private static final String QUEUE_PREFIX = PREFIX + "queue.";

    public static final String QUEUE_CREATED = QUEUE_PREFIX + "created";
    public static final String QUEUE_UPDATED = QUEUE_PREFIX + "updated";
    public static final String QUEUE_DELETED = QUEUE_PREFIX + "deleted";
    public static final String QUEUE_TRANSFER = QUEUE_PREFIX + "transfer";
    public static final String QUEUE_STATUS_UPDATED = QUEUE_PREFIX + "status.updated";
    public static final String QUEUE_SERVER_ASSIGNED = QUEUE_PREFIX + "server.assigned";
}

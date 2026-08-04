package net.mythicisland.queue.api.internal.event;

public final class QueueEventSubjects {

    private QueueEventSubjects() {
    }

    private static final String PREFIX = "queue.";

    private static final String TICKET_PREFIX = PREFIX + "ticket.";

    public static final String TICKET_CREATED = TICKET_PREFIX + "created";
    public static final String TICKET_STATE_CHANGED = TICKET_PREFIX + "state.changed";
    public static final String TICKET_DELETED = TICKET_PREFIX + "deleted";

    private static final String MATCH_PREFIX = PREFIX + "match.";

    public static final String MATCH_CREATED = MATCH_PREFIX + "created";
    public static final String MATCH_STATE_CHANGED = MATCH_PREFIX + "state.changed";
    public static final String MATCH_TRANSFERRED = MATCH_PREFIX + "transferred";
}

package net.mythicisland.queue.shared.nats

/**
 * The NATS subjects queue publishes its events on.
 */
object Subjects {

    private const val PREFIX = "queue"

    private const val TICKET_PREFIX = "${PREFIX}.ticket"

    const val TICKET_CREATED = "${TICKET_PREFIX}.created"

    const val TICKET_STATE_CHANGED = "${TICKET_PREFIX}.state.changed"

    const val TICKET_DELETED = "${TICKET_PREFIX}.deleted"

    private const val MATCH_PREFIX = "${PREFIX}.match"

    const val MATCH_CREATED = "${MATCH_PREFIX}.created"

    const val MATCH_STATE_CHANGED = "${MATCH_PREFIX}.state.changed"

    const val MATCH_TRANSFERRED = "${MATCH_PREFIX}.transferred"

}

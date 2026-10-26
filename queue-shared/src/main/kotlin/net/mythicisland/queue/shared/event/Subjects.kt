package net.mythicisland.queue.shared.event

object Subjects {

    const val TICKET_CREATED = "queue.v1.ticket.created"
    const val TICKET_STATE_CHANGED = "queue.v1.ticket.state_changed"
    const val TICKET_DELETED = "queue.v1.ticket.deleted"

    const val MATCH_CREATED = "queue.v1.match.created"
    const val MATCH_STATE_CHANGED = "queue.v1.match.state_changed"
    const val MATCH_TRANSFERRED = "queue.v1.match.transferred"
    const val MATCH_RELEASED = "queue.v1.match.released"

    fun server(serverId: String, matchSubject: String): String =
        "queue.v1.server.$serverId.${matchSubject.removePrefix("queue.v1.")}"

}

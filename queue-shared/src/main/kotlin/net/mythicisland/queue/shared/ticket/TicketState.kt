package net.mythicisland.queue.shared.ticket

import build.buf.gen.mythicisland.queue.v1.TicketState as TicketStateProto

enum class TicketState {
    SEARCHING,
    MATCHED,
    ASSIGNED;

    fun toProto(): TicketStateProto = when (this) {
        SEARCHING -> TicketStateProto.TICKET_STATE_SEARCHING
        MATCHED -> TicketStateProto.TICKET_STATE_MATCHED
        ASSIGNED -> TicketStateProto.TICKET_STATE_ASSIGNED
    }

    companion object {
        fun fromProto(proto: TicketStateProto): TicketState = when (proto) {
            TicketStateProto.TICKET_STATE_SEARCHING -> SEARCHING
            TicketStateProto.TICKET_STATE_MATCHED -> MATCHED
            TicketStateProto.TICKET_STATE_ASSIGNED -> ASSIGNED
            else -> throw IllegalArgumentException("Unknown ticket state $proto")
        }
    }

}

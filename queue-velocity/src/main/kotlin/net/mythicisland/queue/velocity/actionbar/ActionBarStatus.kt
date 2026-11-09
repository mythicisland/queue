package net.mythicisland.queue.velocity.actionbar

import net.mythicisland.queue.api.ticket.Ticket
import net.mythicisland.queue.api.ticket.TicketState
import java.time.Duration
import java.time.Instant

sealed interface ActionBarStatus {

    data class Searching(val queueTypes: List<String>, val elapsed: Duration) : ActionBarStatus

    data object Matched : ActionBarStatus

    data class Assigned(val serverName: String, val remaining: Duration) : ActionBarStatus

    companion object {

        fun of(ticket: Ticket, now: Instant): ActionBarStatus {
            val assignment = ticket.assignment
            return when (ticket.state) {
                TicketState.SEARCHING -> Searching(ticket.queueTypes, positive(Duration.between(ticket.createdAt, now)))
                TicketState.MATCHED -> Matched
                TicketState.ASSIGNED -> {
                    if (assignment == null) {
                        return Matched
                    }
                    val countdownEndsAt = ticket.countdownEndsAt ?: now
                    Assigned(assignment.serverName, positive(Duration.between(now, countdownEndsAt)))
                }
            }
        }

        fun formatElapsed(duration: Duration): String {
            val hours = duration.toHours()
            val minutes = duration.toMinutesPart()
            val seconds = duration.toSecondsPart()
            if (hours > 0) {
                return "%d:%02d:%02d".format(hours, minutes, seconds)
            }
            return "%d:%02d".format(minutes, seconds)
        }

        fun formatRemaining(duration: Duration): String {
            val seconds = (duration.toMillis() + 999) / 1000
            return "${seconds}s"
        }

        private fun positive(duration: Duration): Duration =
            if (duration.isNegative) Duration.ZERO else duration

    }

}

package net.mythicisland.queue.runtime.match

import net.mythicisland.queue.shared.match.Team
import net.mythicisland.queue.shared.ticket.Ticket

data class TicketSelection(
    val tickets: List<Ticket>,
    val teams: List<Team>,
)

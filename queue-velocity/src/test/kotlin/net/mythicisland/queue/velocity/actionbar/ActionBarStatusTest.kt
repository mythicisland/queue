package net.mythicisland.queue.velocity.actionbar

import net.mythicisland.queue.api.match.Assignment
import net.mythicisland.queue.api.ticket.Ticket
import net.mythicisland.queue.api.ticket.TicketState
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ActionBarStatusTest {

    private val start = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `searching shows the time since the ticket was created`() {
        val status = ActionBarStatus.of(ticket(TicketState.SEARCHING), start.plusSeconds(83))

        assertEquals(ActionBarStatus.Searching(listOf("ffa"), Duration.ofSeconds(83)), status)
    }

    @Test
    fun `searching never shows a negative time when clocks drift`() {
        val status = ActionBarStatus.of(ticket(TicketState.SEARCHING), start.minusSeconds(2))

        assertEquals(ActionBarStatus.Searching(listOf("ffa"), Duration.ZERO), status)
    }

    @Test
    fun `searching lists every queue type of the ticket`() {
        val status = ActionBarStatus.of(ticket(TicketState.SEARCHING, queueTypes = listOf("ffa", "teams")), start)

        assertEquals(ActionBarStatus.Searching(listOf("ffa", "teams"), Duration.ZERO), status)
    }

    @Test
    fun `assigned counts down to the end of the countdown`() {
        val ticket = ticket(TicketState.ASSIGNED, assignment = Assignment("id", "ffa-1"), countdownEndsAt = start.plusSeconds(10))

        assertEquals(ActionBarStatus.Assigned("ffa-1", Duration.ofSeconds(7)), ActionBarStatus.of(ticket, start.plusSeconds(3)))
    }

    @Test
    fun `assigned stays at zero once the countdown is over`() {
        val ticket = ticket(TicketState.ASSIGNED, assignment = Assignment("id", "ffa-1"), countdownEndsAt = start)

        assertEquals(ActionBarStatus.Assigned("ffa-1", Duration.ZERO), ActionBarStatus.of(ticket, start.plusSeconds(5)))
    }

    @Test
    fun `assigned without a server falls back to matched`() {
        assertEquals(ActionBarStatus.Matched, ActionBarStatus.of(ticket(TicketState.ASSIGNED), start))
    }

    @Test
    fun `elapsed time is shown as minutes and seconds`() {
        assertEquals("1:05", ActionBarStatus.formatElapsed(Duration.ofSeconds(65)))
    }

    @Test
    fun `elapsed time shows hours after an hour`() {
        assertEquals("1:02:03", ActionBarStatus.formatElapsed(Duration.ofSeconds(3723)))
    }

    @Test
    fun `remaining time rounds up to the next second`() {
        assertEquals("5s", ActionBarStatus.formatRemaining(Duration.ofMillis(4200)))
    }

    @Test
    fun `remaining time keeps full seconds`() {
        assertEquals("5s", ActionBarStatus.formatRemaining(Duration.ofSeconds(5)))
    }

    private fun ticket(
        state: TicketState,
        queueTypes: List<String> = listOf("ffa"),
        assignment: Assignment? = null,
        countdownEndsAt: Instant? = null,
    ) = Ticket(
        id = UUID.randomUUID(),
        playerIds = listOf(UUID.randomUUID()),
        queueTypes = queueTypes,
        state = state,
        createdAt = start,
        matchId = null,
        assignment = assignment,
        countdownEndsAt = countdownEndsAt,
        properties = emptyMap(),
    )

}

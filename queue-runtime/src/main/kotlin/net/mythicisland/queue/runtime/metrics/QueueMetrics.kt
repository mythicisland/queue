package net.mythicisland.queue.runtime.metrics

import build.buf.gen.mythicisland.queue.v1.MatchReleaseReason
import build.buf.gen.mythicisland.queue.v1.TicketDeleteReason
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import net.mythicisland.queue.runtime.metrics.MetricAttributes.QUEUE_TYPE
import net.mythicisland.queue.runtime.metrics.MetricAttributes.REASON
import net.mythicisland.queue.runtime.metrics.MetricAttributes.RESULT
import net.mythicisland.queue.runtime.metrics.MetricAttributes.STATE
import net.mythicisland.queue.shared.match.Match
import net.mythicisland.queue.shared.match.MatchState
import net.mythicisland.queue.shared.ticket.Ticket
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit

class QueueMetrics(meter: Meter) {

    private val ticketsCreated = meter.counterBuilder("queue.tickets.created")
        .setDescription("Tickets put into the queue")
        .setUnit("{ticket}")
        .build()

    private val ticketsDeleted = meter.counterBuilder("queue.tickets.deleted")
        .setDescription("Tickets removed from the queue")
        .setUnit("{ticket}")
        .build()

    private val ticketPlayers = meter.histogramBuilder("queue.ticket.players")
        .setDescription("Players per ticket")
        .setUnit("{player}")
        .ofLongs()
        .setExplicitBucketBoundariesAdvice(PLAYER_BUCKETS)
        .build()

    private val waitTime = meter.histogramBuilder("queue.ticket.wait_time")
        .setDescription("Time from creating a ticket until it is put into a match")
        .setUnit("s")
        .setExplicitBucketBoundariesAdvice(WAIT_BUCKETS)
        .build()

    private val matchesCreated = meter.counterBuilder("queue.matches.created")
        .setDescription("Matches created by the matchmaker")
        .setUnit("{match}")
        .build()

    private val matchesFinished = meter.counterBuilder("queue.matches.finished")
        .setDescription("Matches that completed, failed or were cancelled")
        .setUnit("{match}")
        .build()

    private val matchesReleased = meter.counterBuilder("queue.matches.released")
        .setDescription("Completed matches whose server slot was freed")
        .setUnit("{match}")
        .build()

    private val matchPlayers = meter.histogramBuilder("queue.match.players")
        .setDescription("Players per created match")
        .setUnit("{player}")
        .ofLongs()
        .setExplicitBucketBoundariesAdvice(PLAYER_BUCKETS)
        .build()

    private val allocationDuration = meter.histogramBuilder("queue.match.allocation.duration")
        .setDescription("Time from creating a match until a server took it")
        .setUnit("s")
        .setExplicitBucketBoundariesAdvice(WAIT_BUCKETS)
        .build()

    private val timeToPlay = meter.histogramBuilder("queue.match.time_to_play")
        .setDescription("Time from creating a match until its players were transferred")
        .setUnit("s")
        .setExplicitBucketBoundariesAdvice(WAIT_BUCKETS)
        .build()

    private val offers = meter.counterBuilder("queue.match.offers")
        .setDescription("Answers of shared servers to match offers")
        .setUnit("{offer}")
        .build()

    private val playersTransferred = meter.counterBuilder("queue.players.transferred")
        .setDescription("Players of transferred matches, by whether they reached the server")
        .setUnit("{player}")
        .build()

    private val transferDuration = meter.histogramBuilder("queue.transfer.duration")
        .setDescription("Time it took to transfer the players of a match")
        .setUnit("s")
        .setExplicitBucketBoundariesAdvice(FAST_BUCKETS)
        .build()

    private val matchmakerDuration = meter.histogramBuilder("queue.matchmaker.duration")
        .setDescription("Time of one matchmaker run over all queue types")
        .setUnit("s")
        .setExplicitBucketBoundariesAdvice(FAST_BUCKETS)
        .build()

    private val reconcilerDuration = meter.histogramBuilder("queue.reconciler.duration")
        .setDescription("Time of one reconciler run over all matches")
        .setUnit("s")
        .setExplicitBucketBoundariesAdvice(FAST_BUCKETS)
        .build()

    fun recordTicketCreation(ticket: Ticket) {
        for (queueType in ticket.queueTypes) {
            val attributes = Attributes.of(QUEUE_TYPE, queueType)
            ticketsCreated.add(1, attributes)
            ticketPlayers.record(ticket.getPlayerCount().toLong(), attributes)
        }
    }

    fun recordTicketDeletion(reason: TicketDeleteReason) {
        ticketsDeleted.add(1, Attributes.of(REASON, reason.name.removePrefix("TICKET_DELETE_REASON_").lowercase()))
    }

    fun recordMatchCreation(match: Match) {
        val attributes = Attributes.of(QUEUE_TYPE, match.queueType)
        matchesCreated.add(1, attributes)
        matchPlayers.record(match.tickets.sumOf { it.getPlayerCount() }.toLong(), attributes)
        for (ticket in match.tickets) {
            waitTime.record(secondsBetween(ticket.createdAt, match.createdAt), attributes)
        }
    }

    fun recordMatchTransition(match: Match) {
        val now = Instant.now()
        val attributes = Attributes.of(QUEUE_TYPE, match.queueType)
        when (match.state) {
            MatchState.COUNTDOWN -> allocationDuration.record(secondsBetween(match.createdAt, now), attributes)
            MatchState.COMPLETED -> timeToPlay.record(secondsBetween(match.createdAt, now), attributes)
            else -> Unit
        }

        if (match.state in FINISHED_STATES) {
            matchesFinished.add(1, Attributes.of(QUEUE_TYPE, match.queueType, STATE, match.state.name.lowercase()))
        }
    }

    fun recordOfferAccepted(match: Match) {
        offers.add(1, Attributes.of(QUEUE_TYPE, match.queueType, RESULT, "accepted"))
    }

    fun recordOfferRejected(match: Match, expired: Boolean) {
        offers.add(1, Attributes.of(QUEUE_TYPE, match.queueType, RESULT, if (expired) "expired" else "rejected"))
    }

    fun recordTransfer(match: Match, transferred: Int, missing: Int, duration: Duration) {
        playersTransferred.add(transferred.toLong(), Attributes.of(QUEUE_TYPE, match.queueType, RESULT, "transferred"))
        playersTransferred.add(missing.toLong(), Attributes.of(QUEUE_TYPE, match.queueType, RESULT, "missing"))
        transferDuration.record(duration.toDouble(DurationUnit.SECONDS), Attributes.of(QUEUE_TYPE, match.queueType))
    }

    fun recordMatchRelease(match: Match, reason: MatchReleaseReason) {
        val name = reason.name.removePrefix("MATCH_RELEASE_REASON_").lowercase()
        matchesReleased.add(1, Attributes.of(QUEUE_TYPE, match.queueType, REASON, name))
    }

    fun recordMatchmakerRun(duration: Duration) {
        matchmakerDuration.record(duration.toDouble(DurationUnit.SECONDS))
    }

    fun recordReconcilerRun(duration: Duration) {
        reconcilerDuration.record(duration.toDouble(DurationUnit.SECONDS))
    }

    private fun secondsBetween(from: Instant, to: Instant): Double = java.time.Duration.between(from, to).toMillis() / 1000.0

    private companion object {
        val FINISHED_STATES = setOf(MatchState.COMPLETED, MatchState.FAILED, MatchState.CANCELLED)
        val WAIT_BUCKETS = listOf(1.0, 2.0, 5.0, 10.0, 15.0, 30.0, 45.0, 60.0, 90.0, 120.0, 180.0, 300.0, 600.0)
        val FAST_BUCKETS = listOf(0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0)
        val PLAYER_BUCKETS = listOf(1L, 2L, 3L, 4L, 5L, 6L, 8L, 10L, 12L, 16L, 20L, 24L, 32L, 48L, 64L, 100L)
    }

}

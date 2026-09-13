package net.mythicisland.queue.runtime.match

import app.simplecloud.api.CloudApi
import build.buf.gen.mythicisland.queue.v2.MatchState
import build.buf.gen.mythicisland.queue.v2.TicketDeleteReason
import build.buf.gen.mythicisland.queue.v2.TicketState
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.repository.MatchRepository
import net.mythicisland.queue.runtime.repository.QueueTypeRepository
import net.mythicisland.queue.runtime.server.ServerAllocator
import net.mythicisland.queue.runtime.ticket.TicketStore
import net.mythicisland.queue.shared.match.Assignment
import net.mythicisland.queue.shared.match.Match
import org.apache.logging.log4j.LogManager
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class MatchReconciler(
    private val tickets: TicketStore,
    private val matches: MatchRepository,
    private val types: QueueTypeRepository,
    private val allocator: ServerAllocator,
    private val api: CloudApi,
    private val publisher: EventPublisher,
    meter: Meter,
) {

    private val logger = LogManager.getLogger(MatchReconciler::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Metrics
    private val stateTransitions = meter.counterBuilder("match.state.transitions").build()
    private val matchFailed = meter.counterBuilder("match.failed").build()
    private val transferDuration = meter.histogramBuilder("match.transfer.duration").setUnit("ms").build()
    private val playersTransferred = meter.counterBuilder("match.transfer.players").build()

    /**
     * Starts the reconciliation loop.
     */
    fun start() {
        logger.info("Starting up match reconciler")
        scope.launch {
            while (isActive) {
                delay(500.milliseconds)
                reconcileAllMatches()
            }
        }
    }

    /**
     * Stops the reconciliation loop.
     */
    fun shutdown() {
        logger.info("Shutting down match reconciler...")
        scope.cancel()
    }

    /**
     * Reconciles all matches in the repository.
     */
    private suspend fun reconcileAllMatches() {
        matches.getAllMatches().forEach { match ->
            try {
                reconcile(match)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to reconcile match {}", match.id, e)
            }
        }
    }

    private suspend fun reconcile(match: Match) {
        // Everyone left while the match was still being set up.
        if (match.ticketIds.isEmpty() && match.state != MatchState.MATCH_STATE_COMPLETED) {
            fail(match, "every ticket left the match")
            return
        }

        when (match.state) {
            MatchState.MATCH_STATE_ALLOCATING -> handleAllocating(match)
            MatchState.MATCH_STATE_COUNTDOWN -> handleCountdown(match)
            MatchState.MATCH_STATE_TRANSFERRING -> handleTransferring(match)
            MatchState.MATCH_STATE_COMPLETED, MatchState.MATCH_STATE_FAILED -> cleanup(match)
            else -> throw IllegalStateException()
        }
    }

    /**
     * Looks for a server. Once one is reserved the countdown starts and the
     * tickets learn where they are going.
     */
    private suspend fun handleAllocating(match: Match) {
        val type = types.find(match.queueType)
        if (type == null) {
            fail(match, "queue type '${match.queueType}' does not exist anymore")
            return
        }

        val waited = Duration.between(match.createdAt, Instant.now()).seconds
        if (waited >= 60L) {
            fail(match, "no server became available within ${60L}s")
            return
        }

        val assignment = allocator.allocate(match, type) ?: return

        val countdownEndsAt = Instant.now().plusSeconds(type.countdownDurationSeconds)
        logger.info("Match {} starts in {}s on server {}", match.id, type.countdownDurationSeconds, assignment.serverName)

        val ready = match.copy(assignment = assignment, countdownEndsAt = countdownEndsAt)
        assignTickets(ready, assignment, countdownEndsAt)
        transition(ready, MatchState.MATCH_STATE_COUNTDOWN)
    }

    /**
     * Waits for the countdown to run out.
     */
    private suspend fun handleCountdown(match: Match) {
        val countdownEndsAt = match.countdownEndsAt
        if (countdownEndsAt == null) {
            fail(match, "countdown state without a countdown")
            return
        }

        if (Instant.now().isBefore(countdownEndsAt)) return

        logger.info("Match {} countdown finished", match.id)
        transition(match, MatchState.MATCH_STATE_TRANSFERRING)
    }

    /**
     * Sends every player to the allocated server.
     */
    private suspend fun handleTransferring(match: Match) {
        val assignment = match.assignment
        if (assignment == null) {
            fail(match, "transfer state without a server")
            return
        }

        val matchTickets = tickets.getAll(match.ticketIds)
        val playerIds = matchTickets.flatMap { it.playerIds }
        logger.info("Match {} transferring {} players to {}", match.id, playerIds.size, assignment.serverName)

        val start = System.nanoTime()
        val transferred = coroutineScope {
            playerIds.map { async { transfer(it, assignment) } }.awaitAll()
        }.filterNotNull()
        transferDuration.record((System.nanoTime() - start) / 1_000_000.0)

        playersTransferred.add(transferred.size.toLong(), Attributes.of(AttributeKey.stringKey("success"), "true"))
        playersTransferred.add((playerIds.size - transferred.size).toLong(), Attributes.of(AttributeKey.stringKey("success"), "false"))

        logger.info("Match {} transferred {}/{} players", match.id, transferred.size, playerIds.size)
        publisher.publishMatchTransferred(match, matchTickets, transferred)
        transition(match, MatchState.MATCH_STATE_COMPLETED)
    }

    private suspend fun transfer(playerId: UUID, assignment: Assignment): UUID? {
        try {
            val player = api.player().get(playerId).await()

            if (player == null) {
                logger.error("Player {} has never joined the network", playerId)
                return null
            }

            if (!player.isOnline) {
                logger.error("Player {} is offline", playerId)
            }

            player.connect(assignment.serverName).await()
            return playerId
        } catch (e: Exception) {
            logger.error("Failed to transfer player {} to server {}", playerId, assignment.serverName, e)
            return null
        }
    }

    /**
     * Cleanup a finished match.
     */
    private suspend fun cleanup(match: Match) {
        val matchTickets = tickets.getAll(match.ticketIds)

        if (match.state == MatchState.MATCH_STATE_COMPLETED) {
            matchTickets.forEach { ticket ->
                tickets.remove(ticket.id)
                publisher.publishTicketDeleted(ticket, TicketDeleteReason.TICKET_DELETE_REASON_TRANSFERRED)
            }
        } else {
            allocator.release(match)
            matchTickets.forEach { ticket ->
                val searching = tickets.update(ticket.asSearching()) ?: return@forEach
                logger.info("Ticket {} is searching again after match {} failed", ticket.id, match.id)
                publisher.publishTicketStateChanged(searching, ticket.state)
            }
        }

        matches.removeMatch(match.id)
        logger.info("Match {} cleaned up", match.id)
    }

    private suspend fun assignTickets(match: Match, assignment: Assignment, countdownEndsAt: Instant) {
        tickets.getAll(match.ticketIds).forEach { ticket ->
            val assigned = ticket.copy(
                state = TicketState.TICKET_STATE_ASSIGNED,
                assignment = assignment,
                countdownEndsAt = countdownEndsAt,
            )

            tickets.update(assigned) ?: return@forEach
            publisher.publishTicketStateChanged(assigned, ticket.state)
        }
    }

    private suspend fun transition(match: Match, state: MatchState) {
        val updatedMatch = matches.updateMatch(match.copy(state = state)) ?: return

        stateTransitions.add(1, Attributes.of(
            AttributeKey.stringKey("from"), match.state.name,
            AttributeKey.stringKey("to"), state.name,
        ))

        logger.info("Match {} state: {} -> {}", match.id, match.state, state)
        publisher.publishMatchStateChanged(updatedMatch, tickets.getAll(updatedMatch.ticketIds), match.state)
    }

    private suspend fun fail(match: Match, reason: String) {
        matchFailed.add(1, Attributes.of(AttributeKey.stringKey("reason"), reason))
        logger.warn("Match {} failed: {}", match.id, reason)
        transition(match, MatchState.MATCH_STATE_FAILED)
    }

}

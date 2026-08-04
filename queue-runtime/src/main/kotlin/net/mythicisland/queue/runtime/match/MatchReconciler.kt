package net.mythicisland.queue.runtime.match

import app.simplecloud.api.CloudApi
import build.buf.gen.mythicisland.queue.v2.MatchState
import build.buf.gen.mythicisland.queue.v2.TicketDeleteReason
import build.buf.gen.mythicisland.queue.v2.TicketState
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

/**
 * Drives a match from the moment it was formed until its players are on the game server.
 */
class MatchReconciler(
    private val tickets: TicketStore,
    private val matches: MatchRepository,
    private val types: QueueTypeRepository,
    private val allocator: ServerAllocator,
    private val api: CloudApi,
    private val publisher: EventPublisher,
) {

    private val logger = LogManager.getLogger(MatchReconciler::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Starts the reconciliation loop.
     */
    fun start() {
        logger.info("Starting up match reconciler")
        scope.launch {
            while (isActive) {
                delay(500.milliseconds)
                tick()
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
     * Reconciles every active match once.
     */
    suspend fun tick() {
        matches.getAll().forEach { match ->
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
            MatchState.MATCH_STATE_COMPLETED, MatchState.MATCH_STATE_FAILED -> cleanUp(match)
            else -> logger.warn("Match {} has unhandled state {}, skipping", match.id, match.state)
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
        logger.info(
            "Match {} starts in {}s on server {}",
            match.id, type.countdownDurationSeconds, assignment.serverName,
        )

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

        val transferred = coroutineScope {
            playerIds.map { async { transfer(it, assignment) } }.awaitAll()
        }.filterNotNull()

        logger.info("Match {} transferred {}/{} players", match.id, transferred.size, playerIds.size)
        publisher.publishMatchTransferred(match, matchTickets, transferred)
        transition(match, MatchState.MATCH_STATE_COMPLETED)
    }

    /**
     * Connects a single player.
     *
     * @return the player id if the transfer worked, null otherwise.
     */
    private suspend fun transfer(playerId: UUID, assignment: Assignment): UUID? {
        try {
            val player = api.player().get(playerId).await()
            if (player == null) {
                logger.warn("Player {} is offline, skipping transfer to {}", playerId, assignment.serverName)
                return null
            }

            player.connect(assignment.serverName).await()
            return playerId
        } catch (e: Exception) {
            logger.error("Failed to transfer player {} to server {}", playerId, assignment.serverName, e)
            return null
        }
    }

    /**
     * Removes a finished match.
     *
     * Completed matches take their tickets with them, failed ones put the
     * players back into matchmaking so nobody gets stuck.
     */
    private suspend fun cleanUp(match: Match) {
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

        matches.remove(match.id)
        logger.info("Match {} cleaned up ({})", match.id, match.state)
    }

    /**
     * Mirrors the server and the countdown onto the tickets of a match, so a
     * consumer never has to load the match to show them.
     */
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

    /**
     * Moves a match into a new state and tells everyone about it.
     */
    private suspend fun transition(match: Match, state: MatchState) {
        val updated = matches.update(match.copy(state = state))
        if (updated == null) {
            logger.debug("Match {} vanished before it could move to {}", match.id, state)
            return
        }

        logger.info("Match {} state: {} -> {}", match.id, match.state, state)
        publisher.publishMatchStateChanged(updated, tickets.getAll(updated.ticketIds), match.state)
    }

    private suspend fun fail(match: Match, reason: String) {
        logger.warn("Match {} failed: {}", match.id, reason)
        transition(match, MatchState.MATCH_STATE_FAILED)
    }

}

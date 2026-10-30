package net.mythicisland.queue.runtime.match

import build.buf.gen.mythicisland.queue.v1.MatchReleaseReason
import build.buf.gen.mythicisland.queue.v1.TicketDeleteReason
import kotlinx.coroutines.*
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.metrics.QueueMetrics
import net.mythicisland.queue.runtime.server.GameServer
import net.mythicisland.queue.runtime.server.ServerAllocator
import net.mythicisland.queue.runtime.server.ServerRegistry
import net.mythicisland.queue.runtime.ticket.TicketStore
import net.mythicisland.queue.runtime.transfer.PlayerTransferService
import net.mythicisland.queue.runtime.type.QueueTypeRegistry
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import net.mythicisland.queue.shared.match.Assignment
import net.mythicisland.queue.shared.match.Match
import net.mythicisland.queue.shared.match.MatchState
import net.mythicisland.queue.shared.ticket.Ticket
import net.mythicisland.queue.shared.ticket.TicketState
import net.mythicisland.queue.shared.type.QueueType
import net.mythicisland.queue.shared.type.SharedMode
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * The reconciler compares the state of every match in the [MatchStore] with the servers
 * in the [ServerRegistry] and runs the next step for it, every 500ms it does the following:
 *
 * - fetch all matches from the store
 *
 * - matches with state [MatchState.ALLOCATING]:
 *   -> if the allocation timeout is reached, set state to [MatchState.FAILED]
 *   -> drop tickets that left, cancel if there are not enough players anymore
 *   -> exclusive group: get a free server and start the countdown
 *   -> shared group: offer the match to a server and set state to [MatchState.OFFERED]
 *
 * - matches with state [MatchState.OFFERED]:
 *   -> if the server accepts, start the countdown
 *   -> if the server rejects or doesn't answer in time, go back to [MatchState.ALLOCATING]
 *      and don't offer it to that server again
 *
 * - matches with state [MatchState.COUNTDOWN]:
 *   -> when the countdown is over, set state to [MatchState.TRANSFERRING]
 *   -> if there are not enough players left, set state to [MatchState.CANCELLED]
 *
 * - matches with state [MatchState.TRANSFERRING]:
 *   -> transfer the players, delete their tickets and set state to [MatchState.COMPLETED]
 */
class MatchReconciler(
    private val types: QueueTypeRegistry,
    private val ticketStore: TicketStore,
    private val matchStore: MatchStore,
    private val registry: ServerRegistry,
    private val allocator: ServerAllocator,
    private val service: PlayerTransferService,
    private val events: EventPublisher,
    private val metrics: QueueMetrics,
) {

    private val logger = LoggerFactory.getLogger(MatchReconciler::class)
    private val offerDeadlines = ConcurrentHashMap<String, Instant>()
    private val rejections = ConcurrentHashMap<String, Set<String>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            while (isActive) {
                reconcile()
                delay(500.milliseconds)
            }
        }
    }

    fun shutdown() {
        scope.cancel()
    }

    fun isRejected(matchId: String, serverId: String): Boolean = rejections[matchId]?.contains(serverId) == true

    suspend fun accept(match: Match, serverId: String): Match? {
        val assignment = match.assignment ?: return null
        if (match.state != MatchState.OFFERED || assignment.serverId != serverId) {
            return null
        }

        val type = types.get(match.queueType) ?: return null
        val accepted = startCountdown(match, type, assignment)
        if (accepted == null) {
            val current = matchStore.get(match.id) ?: return null
            return if (current != match) accept(current, serverId) else null
        }

        offerDeadlines.remove(match.id)
        metrics.recordOfferAccepted(match)
        return accepted
    }

    suspend fun reject(match: Match, serverId: String, reason: String): Boolean {
        if (match.state != MatchState.OFFERED || match.assignment?.serverId != serverId) {
            return false
        }
        if (!transition(match, match.copy(state = MatchState.ALLOCATING, assignment = null))) {
            val current = matchStore.get(match.id) ?: return false
            return current != match && reject(current, serverId, reason)
        }

        rejections.merge(match.id, setOf(serverId)) { old, new -> old + new }
        offerDeadlines.remove(match.id)
        registry.removeMatch(serverId, match.id)
        metrics.recordOfferRejected(match, expired = reason == ACCEPT_TIMEOUT)

        logger.info("Match offer rejected", Field("matchId", match.id), Field("serverId", serverId), Field("reason", reason))
        return true
    }

    suspend fun release(match: Match, reason: MatchReleaseReason) {
        val released = matchStore.remove(match.id) ?: return
        clear(released.id)
        allocator.free(released)

        events.matchReleased(released, reason)
        metrics.recordMatchRelease(released, reason)
        logger.info("Match released", Field("matchId", released.id), Field("reason", reason))
    }

    suspend fun ticketLeft(ticket: Ticket) {
        val matchId = ticket.matchId ?: return
        val stored = matchStore.get(matchId) ?: return
        val match = if (stored.state == MatchState.ALLOCATING) stored.without(listOf(ticket)) else stored
        if (match != stored && !matchStore.replace(stored, match)) {
            return ticketLeft(ticket)
        }

        val type = types.get(match.queueType) ?: return
        if (match.state.isBeforeTransfer() && players(present(match)) < type.match.minPlayers) {
            finish(match, MatchState.CANCELLED)
        }
    }

    suspend fun ticketUpdated(ticketId: String) {
        val ticket = ticketStore.get(ticketId) ?: return
        val matchId = ticket.matchId ?: return
        val stored = matchStore.get(matchId) ?: return
        if (!stored.state.isBeforeTransfer()) {
            return
        }

        if (!matchStore.replace(stored, withCurrentProperties(stored))) {
            return ticketUpdated(ticketId)
        }
    }

    suspend fun serverStopped(server: GameServer) {
        for (match in matchStore.all().filter { it.assignment?.serverId == server.id }) {
            try {
                serverStopped(match, server.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to handle stopped server", Field("matchId", match.id), Field("serverId", server.id), Field.error(e))
            }
        }
    }

    suspend fun recover() {
        val now = Instant.now()
        for (match in matchStore.all()) {
            try {
                recover(match, now)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to recover match", Field("matchId", match.id), Field("state", match.state), Field.error(e))
            }
        }

        for (ticket in ticketStore.all()) {
            val matchId = ticket.matchId ?: continue
            if (matchStore.get(matchId) == null) {
                updateTicket(ticket, ticket.searching())
            }
        }
    }

    private suspend fun recover(match: Match, now: Instant) {
        val type = types.get(match.queueType)
        if (type == null && match.state.isBeforeTransfer()) {
            cancelUnknownType(match)
            return
        }

        val assignment = match.assignment ?: return
        val server = registry.get(assignment.serverId)
        if (server == null && match.state != MatchState.TRANSFERRING) {
            serverStopped(match, assignment.serverId)
            return
        }

        val shared = type?.server?.shared
        if (server != null && shared != null && match.state != MatchState.COMPLETED) {
            registry.addMatch(server.id, match.id)
        }

        when (match.state) {
            MatchState.OFFERED -> if (shared == null) {
                reject(match, assignment.serverId, "queue type is not shared anymore")
            } else {
                offerDeadlines[match.id] = now.plus(shared.acceptTimeout)
            }
            MatchState.COUNTDOWN -> assignTickets(match, assignment)
            MatchState.TRANSFERRING -> launchCompletion(match, present(match), assignment)
            MatchState.COMPLETED -> if (server != null && match.id !in server.hosted) allocator.host(match)
            else -> Unit
        }
    }

    private suspend fun cancelUnknownType(match: Match) {
        logger.warn("Queue type of match does not exist anymore", Field("matchId", match.id), Field("queueType", match.queueType))
        finish(match, MatchState.CANCELLED)
    }

    private suspend fun serverStopped(match: Match, serverId: String) {
        when (match.state) {
            MatchState.OFFERED -> reject(match, serverId, "server stopped")
            MatchState.COUNTDOWN -> finish(match, MatchState.FAILED)
            MatchState.COMPLETED -> release(match, MatchReleaseReason.MATCH_RELEASE_REASON_SERVER_STOPPED)
            else -> Unit
        }
    }

    private suspend fun reconcile() {
        val started = TimeSource.Monotonic.markNow()
        val now = Instant.now()
        for (match in matchStore.all().sortedBy { it.createdAt }) {
            try {
                reconcile(match, now)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to reconcile match", Field("matchId", match.id), Field("state", match.state), Field.error(e))
            }
        }
        provision()
        retryFailedWrites()
        metrics.recordReconcilerRun(started.elapsedNow())
    }

    private suspend fun reconcile(match: Match, now: Instant) {
        val type = types.get(match.queueType)
        if (type == null) {
            if (match.state.isBeforeTransfer()) {
                cancelUnknownType(match)
            }
            return
        }

        when (match.state) {
            MatchState.ALLOCATING -> allocate(match, type, now)
            MatchState.OFFERED -> if (!now.isBefore(offerDeadlines[match.id] ?: now)) expireOffer(match)
            MatchState.COUNTDOWN -> if (!now.isBefore(match.countdownEndTime ?: now)) transfer(match, type)
            else -> Unit
        }
    }

    private suspend fun allocate(match: Match, type: QueueType, now: Instant) {
        if (!now.isBefore(match.createdAt.plus(type.server.allocationTimeout))) {
            finish(match, MatchState.FAILED)
            return
        }

        val left = match.tickets.filter { !isPresent(it, match) }
        if (left.isNotEmpty()) {
            val remaining = match.without(left)
            if (matchStore.replace(match, remaining) && players(remaining.tickets) < type.match.minPlayers) {
                finish(remaining, MatchState.CANCELLED)
            }
            return
        }

        val synced = withCurrentProperties(match)
        if (synced != match) {
            matchStore.replace(match, synced)
            return
        }

        val shared = type.server.shared
        if (shared == null) {
            allocateExclusive(match, type)
        } else {
            offer(match, shared, type, now)
        }
    }

    private suspend fun allocateExclusive(match: Match, type: QueueType) {
        val server = allocator.allocateExclusive(type.server.group) ?: return
        val assignment = Assignment(server.id, server.name)
        if (startCountdown(match, type, assignment) == null) {
            allocator.free(match.copy(assignment = assignment))
        }
    }

    private suspend fun offer(match: Match, shared: SharedMode, type: QueueType, now: Instant) {
        val server = allocator.offer(type.server.group, match.id, rejections[match.id] ?: emptySet()) ?: return
        offerDeadlines[match.id] = now.plus(shared.acceptTimeout)

        val offered = match.copy(state = MatchState.OFFERED, assignment = Assignment(server.id, server.name))
        if (!transition(match, offered)) {
            offerDeadlines.remove(match.id)
            registry.removeMatch(server.id, match.id)
        }
    }

    private suspend fun expireOffer(match: Match) {
        val serverId = match.assignment?.serverId ?: return
        reject(match, serverId, ACCEPT_TIMEOUT)
    }

    private suspend fun startCountdown(match: Match, type: QueueType, assignment: Assignment): Match? {
        val end = Instant.now().plus(type.match.countdown)
        val counting = match.copy(
            state = MatchState.COUNTDOWN,
            assignment = assignment,
            countdownEndTime = end,
            tickets = match.tickets.map { ticket ->
                if (isPresent(ticket, match)) ticket.assigned(assignment, end) else ticket
            },
        )
        if (!transition(match, counting)) {
            return null
        }

        assignTickets(counting, assignment)
        return counting
    }

    private suspend fun assignTickets(counting: Match, assignment: Assignment) {
        val end = counting.countdownEndTime ?: return
        for (ticket in present(counting)) {
            if (ticket.state == TicketState.MATCHED) {
                updateTicket(ticket, ticket.assigned(assignment, end))
            }
        }
    }

    private suspend fun transfer(match: Match, type: QueueType) {
        val present = present(match)
        if (players(present) < type.match.minPlayers) {
            finish(match, MatchState.CANCELLED)
            return
        }

        val assignment = match.assignment ?: return
        val transferring = match.copy(state = MatchState.TRANSFERRING)
        if (!transition(match, transferring)) {
            return
        }

        launchCompletion(transferring, present, assignment)
    }

    private fun launchCompletion(transferring: Match, present: List<Ticket>, assignment: Assignment) {
        scope.launch {
            try {
                complete(transferring, present, assignment)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to transfer match", Field("matchId", transferring.id), Field.error(e))
            }
        }
    }

    private suspend fun complete(transferring: Match, present: List<Ticket>, assignment: Assignment) {
        val started = TimeSource.Monotonic.markNow()
        val result = service.transfer(present.flatMap { it.playerIds }, assignment.serverName)
        val missing = transferring.tickets.flatMap { it.playerIds } - result.transferred.toSet()
        events.matchTransferred(transferring, result.transferred, missing)
        metrics.recordTransfer(transferring, result.transferred.size, missing.size, started.elapsedNow())

        for (ticket in present) {
            val removed = ticketStore.remove(ticket.id) ?: continue
            events.ticketDeleted(removed, TicketDeleteReason.TICKET_DELETE_REASON_TRANSFERRED)
            metrics.recordTicketDeletion(removed, TicketDeleteReason.TICKET_DELETE_REASON_TRANSFERRED)
        }

        val completed = transferring.copy(state = MatchState.COMPLETED)
        if (!transition(transferring, completed)) {
            return
        }
        allocator.host(completed)

        logger.info(
            "Match transferred",
            Field("matchId", completed.id),
            Field("server", assignment.serverName),
            Field("transferred", result.transferred.size),
            Field("missing", missing.size),
        )

        if (registry.get(assignment.serverId) == null) {
            release(completed, MatchReleaseReason.MATCH_RELEASE_REASON_SERVER_STOPPED)
        }
    }

    private suspend fun finish(match: Match, state: MatchState) {
        if (!match.state.isBeforeTransfer()) {
            return
        }

        val finished = match.copy(state = state)
        if (!transition(match, finished)) {
            return
        }

        matchStore.remove(finished.id)
        clear(finished.id)

        for (ticket in finished.tickets) {
            val current = ticketStore.get(ticket.id) ?: continue
            if (current.matchId == finished.id) {
                updateTicket(current, current.searching())
            }
        }

        allocator.free(finished)
    }

    private suspend fun provision() {
        val waiting = matchStore.all()
            .filter { it.state == MatchState.ALLOCATING }
            .mapNotNull { types.get(it.queueType) }
            .groupBy { it.server.group }

        for ((group, waitingTypes) in waiting) {
            val shared = types.isShared(group)
            try {
                allocator.provision(group, if (shared) 1 else waitingTypes.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to provision servers", Field("group", group), Field.error(e))
            }
        }
    }

    private suspend fun retryFailedWrites() {
        try {
            ticketStore.retryFailedWrites()
            matchStore.retryFailedWrites()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to retry state writes", Field.error(e))
        }
    }

    private suspend fun transition(previous: Match, current: Match): Boolean {
        if (!matchStore.replace(previous, current)) {
            return false
        }

        events.matchStateChanged(current, previous)
        metrics.recordMatchTransition(current)
        logger.info("Match state changed", Field("matchId", current.id), Field("from", previous.state), Field("to", current.state))
        return true
    }

    private suspend fun updateTicket(previous: Ticket, current: Ticket) {
        if (ticketStore.replace(previous, current)) {
            events.ticketStateChanged(current, previous.state)
        }
    }

    private fun withCurrentProperties(match: Match): Match = match.copy(
        tickets = match.tickets.map { ticket ->
            val current = ticketStore.get(ticket.id)
            if (current != null && current.matchId == match.id) ticket.copy(properties = current.properties) else ticket
        },
    )

    private fun isPresent(ticket: Ticket, match: Match): Boolean = ticketStore.get(ticket.id)?.matchId == match.id

    private fun present(match: Match): List<Ticket> =
        match.tickets.mapNotNull { ticketStore.get(it.id) }.filter { it.matchId == match.id }

    private fun players(tickets: List<Ticket>): Int = tickets.sumOf { it.getPlayerCount() }

    private fun clear(matchId: String) {
        offerDeadlines.remove(matchId)
        rejections.remove(matchId)
    }

    private companion object {
        const val ACCEPT_TIMEOUT = "accept timeout"
    }

}

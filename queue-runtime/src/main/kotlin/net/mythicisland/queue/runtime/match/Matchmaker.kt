package net.mythicisland.queue.runtime.match

import build.buf.gen.mythicisland.queue.v2.MatchState
import build.buf.gen.mythicisland.queue.v2.TicketState
import kotlinx.coroutines.*
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.repository.MatchRepository
import net.mythicisland.queue.runtime.repository.QueueTypeRepository
import net.mythicisland.queue.runtime.ticket.TicketPool
import net.mythicisland.queue.runtime.ticket.TicketStore
import net.mythicisland.queue.shared.match.Match
import net.mythicisland.queue.shared.match.Ticket
import net.mythicisland.queue.shared.queue.QueueType
import org.apache.logging.log4j.LogManager
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class Matchmaker(
    private val store: TicketStore,
    private val pool: TicketPool,
    private val matches: MatchRepository,
    private val types: QueueTypeRepository,
    private val publisher: EventPublisher,
) {

    private val logger = LogManager.getLogger(Matchmaker::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Starts the matchmaking loop.
     */
    fun start() {
        logger.info("Starting up matchmaker")
        scope.launch {
            while (isActive) {
                delay(500.milliseconds)
                try {
                    tick()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Error in Matchmaking", e)
                }
            }
        }
    }

    /**
     * Stops the matchmaking loop.
     */
    fun shutdown() {
        logger.info("Shutting down matchmaker...")
        scope.cancel()
    }

    suspend fun tick() {
        types.getAll().forEach { type ->
            while (createMatch(type) != null) {
                continue
            }
        }
    }

    private suspend fun createMatch(type: QueueType): Match? {
        val tickets = pool.getAllTickets(type.name)
        val selected = selectTickets(tickets, type, Instant.now()) ?: return null

        val match = Match(
            id = UUID.randomUUID(),
            queueType = type.name,
            ticketIds = selected.map { it.id },
            state = MatchState.MATCH_STATE_ALLOCATING,
            createdAt = Instant.now(),
        )

        val matched = store.matched(match.ticketIds, match.id) ?: return null

        matches.addMatch(match)
        logger.info("Deployed match {} for '{}' with {} tickets and {} players", match.id, type.name, matched.size, matched.sumOf { it.playerIds.size })
        publisher.publishMatchCreated(match, matched)
        matched.forEach { publisher.publishTicketStateChanged(it, TicketState.TICKET_STATE_SEARCHING) }
        return match
    }

    fun selectTickets(tickets: List<Ticket>, type: QueueType, now: Instant): List<Ticket>? {
        if (tickets.isEmpty()) return null

        val selectedTickets = tickets.fold(emptyList<Ticket>()) { picked, ticket ->
            val players = picked.sumOf { it.playerIds.size }
            if (players + ticket.playerIds.size <= type.maxPlayers) picked + ticket else picked
        }

        val players = selectedTickets.sumOf { it.playerIds.size }
        if (players < type.minPlayers) return null
        if (players >= type.maxPlayers) return selectedTickets

        val waited = Duration.between(tickets.first().createdAt, now).seconds
        if (waited < type.waitingDurationSeconds) return null

        return selectedTickets
    }

}

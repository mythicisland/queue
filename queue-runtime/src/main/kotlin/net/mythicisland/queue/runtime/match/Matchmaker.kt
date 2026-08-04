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

/**
 * Forms matches out of the tickets that are waiting.
 *
 * The matchmaker runs a pass over every queue type on a fixed interval. All it
 * does is pick tickets and hand the result to the [MatchRepository], driving
 * the match afterwards is the job of the [MatchReconciler].
 */
class Matchmaker(
    private val tickets: TicketStore,
    private val pool: TicketPool,
    private val matches: MatchRepository,
    private val types: QueueTypeRepository,
    private val publisher: EventPublisher,
) {

    private companion object {
        val INTERVAL = 500.milliseconds
    }

    private val logger = LogManager.getLogger(Matchmaker::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Starts the matchmaking loop.
     */
    fun start() {
        logger.info("Starting up matchmaker (interval={})", INTERVAL)
        scope.launch {
            while (isActive) {
                delay(INTERVAL)
                try {
                    tick()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Matchmaking pass failed", e)
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

    /**
     * Runs one matchmaking pass over every queue type.
     */
    suspend fun tick() {
        types.getAll().forEach { type ->
            // A queue type can fill more than one match per pass when a lot of
            // players are waiting, so keep going until nothing fits anymore.
            while (createMatch(type) != null) {
                continue
            }
        }
    }

    /**
     * Tries to form a single match for a queue type.
     *
     * @return the created match, or null if the queue type cannot start one yet.
     */
    private suspend fun createMatch(type: QueueType): Match? {
        val candidates = pool.searching(type.name)
        val selected = select(candidates, type, Instant.now()) ?: return null

        val match = Match(
            id = UUID.randomUUID(),
            queueType = type.name,
            ticketIds = selected.map { it.id },
            state = MatchState.MATCH_STATE_ALLOCATING,
            createdAt = Instant.now(),
        )

        // Fails when one of the tickets was matched or cancelled in between,
        // the next pass simply tries again with what is left.
        val matched = tickets.matched(match.ticketIds, match.id)
        if (matched == null) {
            logger.debug("Dropped match for '{}', one of its tickets is no longer searching", type.name)
            return null
        }

        matches.add(match)
        logger.info(
            "Created match {} for '{}' with {} tickets / {} players",
            match.id, type.name, matched.size, matched.sumOf { it.playerCount },
        )

        publisher.publishMatchCreated(match, matched)
        matched.forEach { publisher.publishTicketStateChanged(it, TicketState.TICKET_STATE_SEARCHING) }
        return match
    }

    /**
     * Picks the tickets for the next match of a queue type.
     *
     * A match is formed when it is full, or when it holds at least the minimum
     * amount of players and the oldest ticket waited long enough. The waiting
     * time comes from the ticket itself, so there is no countdown to keep track
     * of anywhere.
     *
     * @param candidates the searching tickets of the queue type, oldest first.
     * @param type the queue type to form a match for.
     * @param now the current time.
     * @return the picked tickets, or null if no match can be formed yet.
     */
    fun select(candidates: List<Ticket>, type: QueueType, now: Instant): List<Ticket>? {
        if (candidates.isEmpty()) return null

        val selected = candidates.fold(emptyList<Ticket>()) { picked, ticket ->
            // Parties that do not fit into the remaining slots are skipped, the
            // smaller tickets behind them can still get in.
            val players = picked.sumOf { it.playerCount }
            if (players + ticket.playerCount <= type.maxPlayers) picked + ticket else picked
        }

        val players = selected.sumOf { it.playerCount }
        if (players < type.minPlayers) return null
        if (players >= type.maxPlayers) return selected

        val waited = Duration.between(candidates.first().createdAt, now).seconds
        if (waited < type.waitingDurationSeconds) return null

        return selected
    }

}

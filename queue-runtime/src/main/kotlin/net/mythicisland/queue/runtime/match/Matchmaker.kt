package net.mythicisland.queue.runtime.match

import kotlinx.coroutines.*
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.ticket.TicketStore
import net.mythicisland.queue.runtime.type.QueueTypeRegistry
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import net.mythicisland.queue.shared.match.Match
import net.mythicisland.queue.shared.match.MatchState
import net.mythicisland.queue.shared.ticket.TicketState
import net.mythicisland.queue.shared.type.QueueType
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class Matchmaker(
    private val types: QueueTypeRegistry,
    private val ticketStore: TicketStore,
    private val matchStore: MatchStore,
    private val events: EventPublisher,
) {

    private val logger = LoggerFactory.getLogger(Matchmaker::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            while (isActive) {
                for (type in types.all()) {
                    try {
                        matchmake(type)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error("Matchmaking failed", Field("queueType", type.name), Field.error(e))
                    }
                }
                delay(500.milliseconds)
            }
        }
    }

    suspend fun shutdown() {
        scope.coroutineContext.job.cancelAndJoin()
    }

    private suspend fun matchmake(type: QueueType) {
        while (true) {
            val now = Instant.now()
            val selected = TicketSelector.select(type, ticketStore.searching(type.name), now) ?: return
            val teams = TeamAssigner.assign(type, selected) ?: return
            val matchId = UUID.randomUUID().toString()
            val claimed = ticketStore.claim(selected.map { it.id }, matchId) ?: continue

            val match = Match(
                id = matchId,
                queueType = type.name,
                tickets = claimed,
                teams = teams,
                state = MatchState.ALLOCATING,
                createdAt = now,
            )
            matchStore.add(match)
            events.matchCreated(match)
            for (ticket in claimed) {
                if (ticketStore.get(ticket.id)?.matchId == matchId) {
                    events.ticketStateChanged(ticket, TicketState.SEARCHING)
                }
            }

            logger.info(
                "Created match",
                Field("matchId", match.id),
                Field("queueType", type.name),
                Field("players", claimed.sumOf { it.getPlayerCount() }),
            )
        }
    }

}

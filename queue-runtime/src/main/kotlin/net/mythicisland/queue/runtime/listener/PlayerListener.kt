package net.mythicisland.queue.runtime.listener

import app.simplecloud.api.CloudApi
import app.simplecloud.api.event.Subscription
import build.buf.gen.mythicisland.queue.v1.TicketDeleteReason
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import net.mythicisland.queue.runtime.ticket.TicketService
import net.mythicisland.queue.runtime.ticket.TicketStore
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import net.mythicisland.queue.shared.ticket.Ticket
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds

class PlayerListener(
    private val api: CloudApi,
    private val store: TicketStore,
    private val service: TicketService,
) {

    private val logger = LoggerFactory.getLogger(PlayerListener::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val subscriptions = mutableListOf<Subscription>()
    private val missing = AtomicReference<Set<String>>(emptySet())

    fun start() {
        subscriptions.add(api.event().player().onDisconnect { event ->
            scope.launch { disconnected(event.playerId) }
        })

        scope.launch {
            while (isActive) {
                delay(30.seconds)
                sync()
            }
        }
    }

    suspend fun shutdown() {
        subscriptions.forEach { it.unsubscribe() }
        scope.coroutineContext.job.cancelAndJoin()
    }

    private suspend fun disconnected(player: UUID) {
        try {
            val ticket = store.getByPlayer(player) ?: return
            service.delete(ticket, TicketDeleteReason.TICKET_DELETE_REASON_DISCONNECTED)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to delete ticket of disconnected player", Field("player", player), Field.error(e))
        }
    }

    private suspend fun sync() {
        try {
            val since = Instant.now()
            val online = api.player().onlinePlayers.await().map { it.uniqueId }.toSet()
            val result = findDisconnected(store.all(), online, missing.get(), since)
            missing.set(result.missing)

            for (ticket in result.disconnected) {
                service.delete(ticket, TicketDeleteReason.TICKET_DELETE_REASON_DISCONNECTED)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to sync tickets with online players", Field.error(e))
        }
    }

    data class SyncResult(val disconnected: List<Ticket>, val missing: Set<String>)

    companion object {

        // A ticket is only dropped when its players are missing in two syncs in a row, because the
        // controller can report too few players for a moment, e.g. right after it restarted.
        fun findDisconnected(tickets: Collection<Ticket>, online: Set<UUID>, missing: Set<String>, since: Instant): SyncResult {
            if (online.isEmpty()) {
                return SyncResult(emptyList(), missing)
            }

            val absent = tickets.filter { ticket ->
                ticket.createdAt.isBefore(since) && ticket.playerIds.any { it !in online }
            }
            val disconnected = absent.filter { it.id in missing }
            val stillMissing = absent.map { it.id }.toSet() - disconnected.map { it.id }.toSet()
            return SyncResult(disconnected, stillMissing)
        }

    }

}

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
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class PlayerListener(
    private val api: CloudApi,
    private val store: TicketStore,
    private val service: TicketService,
) {

    private val logger = LoggerFactory.getLogger(PlayerListener::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val subscriptions = mutableListOf<Subscription>()

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

            for (ticket in store.all()) {
                if (ticket.createdAt.isBefore(since) && ticket.playerIds.any { it !in online }) {
                    service.delete(ticket, TicketDeleteReason.TICKET_DELETE_REASON_DISCONNECTED)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to sync tickets with online players", Field.error(e))
        }
    }

}

package net.mythicisland.queue.runtime.ticket

import build.buf.gen.mythicisland.queue.v1.TicketDeleteReason
import kotlinx.coroutines.*
import net.mythicisland.queue.runtime.type.QueueTypeRegistry
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import net.mythicisland.queue.shared.ticket.TicketState
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class TicketExpirer(
    private val types: QueueTypeRegistry,
    private val store: TicketStore,
    private val service: TicketService,
) {

    private val logger = LoggerFactory.getLogger(TicketExpirer::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            while (isActive) {
                expire()
                delay(1.seconds)
            }
        }
    }

    suspend fun shutdown() {
        scope.coroutineContext.job.cancelAndJoin()
    }

    private suspend fun expire() {
        val now = Instant.now()
        for (ticket in store.all()) {
            try {
                if (ticket.state != TicketState.SEARCHING) {
                    continue
                }
                if (ticket.isExpired(ticket.queueTypes.mapNotNull { types.get(it) }, now)) {
                    service.delete(ticket, TicketDeleteReason.TICKET_DELETE_REASON_EXPIRED)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to expire ticket", Field("ticketId", ticket.id), Field.error(e))
            }
        }
    }

}

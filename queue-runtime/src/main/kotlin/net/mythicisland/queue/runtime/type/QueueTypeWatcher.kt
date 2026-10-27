package net.mythicisland.queue.runtime.type

import kotlinx.coroutines.*
import net.mythicisland.queue.runtime.server.ServerRegistry
import net.mythicisland.queue.runtime.ticket.TicketService
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import net.mythicisland.queue.shared.repository.DirectoryEvent
import net.mythicisland.queue.shared.type.QueueType

class QueueTypeWatcher(
    private val types: QueueTypeRegistry,
    private val registry: ServerRegistry,
    private val ticketService: TicketService,
) {

    private val logger = LoggerFactory.getLogger(QueueTypeWatcher::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            try {
                types.watch().collect { event -> handle(event) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Stopped watching queue types", Field.error(e))
            }
        }
    }

    suspend fun shutdown() {
        scope.coroutineContext.job.cancelAndJoin()
    }

    private suspend fun handle(event: DirectoryEvent<QueueType>) {
        try {
            when (event) {
                is DirectoryEvent.Changed -> changed(event.value.copy(name = event.name))
                is DirectoryEvent.Removed -> removed(event.name)
                is DirectoryEvent.Failed -> logger.error("Failed to read queue type", Field("queueType", event.name), Field.error(event.error))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to apply queue type change", Field("queueType", event.name), Field.error(e))
        }
    }

    private suspend fun changed(type: QueueType) {
        val groups = types.groups()
        if (!types.put(type)) {
            logger.warn("Ignored invalid queue type change", Field("queueType", type.name))
            return
        }

        logger.info("Reloaded queue type", Field("queueType", type.name))
        if (type.server.group !in groups) {
            registry.load(setOf(type.server.group))
        }
    }

    private suspend fun removed(name: String) {
        types.remove(name) ?: return
        logger.info("Removed queue type", Field("queueType", name))
        ticketService.recover()
    }

}

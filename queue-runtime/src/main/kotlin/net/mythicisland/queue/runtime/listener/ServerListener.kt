package net.mythicisland.queue.runtime.listener

import app.simplecloud.api.CloudApi
import app.simplecloud.api.event.Subscription
import app.simplecloud.api.server.Server
import kotlinx.coroutines.*
import net.mythicisland.queue.runtime.match.MatchReconciler
import net.mythicisland.queue.runtime.server.GameServer
import net.mythicisland.queue.runtime.server.ServerRegistry
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import kotlin.time.Duration.Companion.seconds

class ServerListener(
    private val api: CloudApi,
    private val registry: ServerRegistry,
    private val reconciler: MatchReconciler,
) {

    private val logger = LoggerFactory.getLogger(ServerListener::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val subscriptions = mutableListOf<Subscription>()

    suspend fun start(groups: Set<String>) {
        val events = api.event().server()
        subscriptions.add(events.onStarted { changed(it.server) })
        subscriptions.add(events.onUpdated { changed(it.server) })
        subscriptions.add(events.onStateChanged { changed(it.server) })
        subscriptions.add(events.onStopped { stopped(it.serverId) })
        subscriptions.add(events.onDeleted { stopped(it.serverId) })

        registry.load(groups)

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

    private suspend fun sync() {
        try {
            for (server in registry.sync()) {
                logger.info("Game server vanished", Field("serverId", server.id), Field("server", server.name))
                reconciler.serverStopped(server)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to sync game servers", Field.error(e))
        }
    }

    private fun changed(server: Server) {
        val state = ServerRegistry.stateOf(server)
        if (state == null || state in ServerRegistry.STOPPED_STATES) {
            stopped(server.serverId)
            return
        }
        registry.update(server)
    }

    private fun stopped(serverId: String) {
        val server = registry.remove(serverId) ?: return
        logger.info("Game server stopped", Field("serverId", server.id), Field("server", server.name))
        scope.launch { serverStopped(server) }
    }

    private suspend fun serverStopped(server: GameServer) {
        try {
            reconciler.serverStopped(server)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to handle stopped server", Field("serverId", server.id), Field.error(e))
        }
    }

}

package net.mythicisland.queue.runtime.server

import app.simplecloud.api.CloudApi
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mythicisland.queue.runtime.type.QueueTypeRegistry
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import net.mythicisland.queue.shared.server.ServerProperties
import java.util.concurrent.ConcurrentHashMap

class ServerRegistry(private val api: CloudApi, private val types: QueueTypeRegistry) {

    private val logger = LoggerFactory.getLogger(ServerRegistry::class)
    private val mutex = Mutex()
    private val groupNames = ConcurrentHashMap<String, String>()
    private val servers = ConcurrentHashMap<String, GameServer>()
    // https://youtrack.jetbrains.com/issue/KT-18053
    private val stopped = ConcurrentHashMap.newKeySet<String>()
    private val missing = ConcurrentHashMap.newKeySet<String>()

    suspend fun load(groups: Set<String>) {
        for (name in groups) {
            val group = api.group().getGroupByName(name).await()
            groupNames[group.serverGroupId] = name
        }
        sync()

        logger.info("Loaded game servers", Field("groups", groups.size), Field("servers", servers.size))
    }

    suspend fun sync(): List<GameServer> {
        val removed = mutableListOf<GameServer>()
        for (group in groupNames.values.toSet()) {
            val known = byGroup(group)
            val running = api.server().getServersByGroup(group).await()
                .filter { server ->
                    val state = stateOf(server)
                    state != null && state !in STOPPED_STATES
                }

            for (server in running) {
                update(server)
            }

            val ids = running.map { it.serverId }.toSet()
            for (server in known) {
                if (server.id in ids) {
                    missing.remove(server.id)
                } else if (!missing.add(server.id)) {
                    removed.add(remove(server.id) ?: continue)
                }
            }
        }
        return removed
    }

    fun get(id: String): GameServer? = servers[id]

    fun byGroup(group: String): List<GameServer> = servers.values.filter { it.group == group }

    fun pending(group: String): Int = byGroup(group).count {
        it.state in STARTING_STATES || (it.state == ServerState.AVAILABLE && it.capacity == null)
    }

    fun update(server: Server) {
        val state = stateOf(server) ?: return
        val groupId = server.serverGroupId
        val group = (if (groupId != null) groupNames[groupId] else null) ?: servers[server.serverId]?.group ?: return

        val previous = servers[server.serverId]
        val updated = servers.compute(server.serverId) { id, current ->
            if (id in stopped) current else toGameServer(server, group, state, current)
        } ?: return

        if (lacksCapacity(updated) && (previous == null || !lacksCapacity(previous))) {
            logger.warn(
                "Shared game server has no capacity and gets no matches",
                Field("serverId", updated.id),
                Field("server", updated.name),
                Field("property", ServerProperties.CAPACITY),
            )
        }
    }

    private fun lacksCapacity(server: GameServer): Boolean =
        server.state == ServerState.AVAILABLE && server.capacity == null && types.isShared(server.group)

    fun remove(id: String): GameServer? {
        stopped.add(id)
        missing.remove(id)
        return servers.remove(id)
    }

    fun addMatch(serverId: String, matchId: String) {
        servers.computeIfPresent(serverId) { _, current -> current.copy(matches = current.matches + matchId) }
    }

    suspend fun hostMatch(serverId: String, matchId: String) = mutex.withLock {
        val server = servers.computeIfPresent(serverId) { _, current ->
            current.copy(matches = current.matches + matchId, hosted = current.hosted + matchId)
        } ?: return@withLock
        writeHosted(server)
    }

    suspend fun removeMatch(serverId: String, matchId: String): Boolean = mutex.withLock {
        val known = servers[serverId] ?: return@withLock false
        if (matchId !in known.matches) {
            return@withLock false
        }

        val server = servers.computeIfPresent(serverId) { _, current ->
            current.copy(matches = current.matches - matchId, hosted = current.hosted - matchId)
        } ?: return@withLock false
        if (matchId in known.hosted) {
            writeHosted(server)
        }
        true
    }

    private suspend fun writeHosted(server: GameServer) {
        try {
            api.server().updateServerProperty(server.id, ServerProperties.MATCHES, server.hosted.toList()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to write hosted matches", Field("serverId", server.id), Field.error(e))
        }
    }

    private fun toGameServer(server: Server, group: String, state: ServerState, known: GameServer?): GameServer {
        val hosted = known?.hosted ?: hosted(server)
        return GameServer(
            id = server.serverId,
            name = known?.name ?: "$group-${server.numericalId}",
            group = group,
            state = state,
            capacity = capacity(server) ?: known?.capacity ?: types.capacity(group),
            matches = known?.matches ?: hosted,
            hosted = hosted,
        )
    }

    private fun capacity(server: Server): Int? = when (val value = server.properties?.get(ServerProperties.CAPACITY)) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    }

    private fun hosted(server: Server): Set<String> = when (val value = server.properties?.get(ServerProperties.MATCHES)) {
        is Collection<*> -> value.map { it.toString() }.toSet()
        else -> emptySet()
    }

    companion object {
        val STOPPED_STATES = setOf(ServerState.STOPPING, ServerState.CLEANUP, ServerState.STOPPED)
        private val STARTING_STATES = setOf(ServerState.QUEUED, ServerState.PREPARING, ServerState.STARTING)

        fun stateOf(server: Server): ServerState? = try {
            server.state
        } catch (_: IllegalStateException) {
            null
        }
    }

}

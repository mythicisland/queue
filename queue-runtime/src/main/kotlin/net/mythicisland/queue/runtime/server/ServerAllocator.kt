package net.mythicisland.queue.runtime.server

import app.simplecloud.api.CloudApi
import app.simplecloud.api.server.ServerState
import app.simplecloud.api.server.UpdateServerRequest
import kotlinx.coroutines.future.await
import net.mythicisland.queue.shared.match.Assignment
import net.mythicisland.queue.shared.match.Match
import net.mythicisland.queue.shared.queue.QueueType
import org.apache.logging.log4j.LogManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ServerAllocator(
    private val api: CloudApi,
) {

    private val logger = LogManager.getLogger(ServerAllocator::class.java)

    private val requested = ConcurrentHashMap.newKeySet<UUID>()

    suspend fun allocate(match: Match, type: QueueType): Assignment? {
        val servers = api.server().getServersByGroup(type.group).await()
        val server = servers.firstOrNull { it.state == ServerState.AVAILABLE }

        if (server == null) {
            requestServer(match, type)
            return null
        }

        if (!updateState(server.serverId, ServerState.INGAME)) {
            logger.error("Failed to reserve server {} for match {}", server.serverId, match.id)
            return null
        }

        requested.remove(match.id)

        val assignment = Assignment(server.serverId, "${server.group.name}-${server.numericalId}")
        logger.info("Assigned server {} for match {}", assignment.serverName, match.id)
        return assignment
    }

    suspend fun release(match: Match) {
        requested.remove(match.id)

        val assignment = match.assignment ?: return

        if (updateState(assignment.serverId, ServerState.AVAILABLE)) {
            logger.info("Released server {} of match {}", assignment.serverName, match.id)
        } else {
            logger.error("Failed to update state for server {} of match {}", assignment.serverName, match.id)
        }
    }

    private suspend fun requestServer(match: Match, type: QueueType) {
        if (!requested.add(match.id)) return

        try {
            val group = api.group().getGroupByName(type.group).await()
            if (group == null) {
                logger.error("Group '{}' of queue type '{}' does not exist", type.group, type.name)
                return
            }

            api.group().requestServerStart(group).await()
            logger.info("Requested a new server in group '{}' for match {}", type.group, match.id)
        } catch (e: Exception) {
            requested.remove(match.id)
            logger.error("Failed to request a server in group '{}' for match {}", type.group, match.id, e)
        }
    }

    private suspend fun updateState(serverId: String, state: ServerState): Boolean {
        return try {
            val request = UpdateServerRequest.builder().state(state).build()
            api.server().updateServer(serverId, request).await()
            true
        } catch (e: Exception) {
            logger.error("Failed to update state from server {} to state {}", serverId, state, e)
            false
        }
    }

}

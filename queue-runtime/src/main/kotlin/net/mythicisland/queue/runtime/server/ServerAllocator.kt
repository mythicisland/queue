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

/**
 * Handle server allocation.
 *
 * @param api the simplecloud api.
 */
class ServerAllocator(
    private val api: CloudApi,
) {

    private val logger = LogManager.getLogger(ServerAllocator::class.java)

    private val requested = ConcurrentHashMap.newKeySet<UUID>()

    /**
     * Tries to take a free server for a match.
     *
     * If nothing is free a new server is requested once and null is returned,
     * the next reconciliation picks it up as soon as it is running.
     *
     * @param match the match that needs a server.
     * @param type the queue type of the match.
     * @return the assignment, or null if no server is ready yet.
     */
    suspend fun allocate(match: Match, type: QueueType): Assignment? {
        val servers = api.server().getServersByGroup(type.group).await()
        val free = servers.firstOrNull { it.state == ServerState.AVAILABLE }

        if (free == null) {
            requestServer(match, type)
            return null
        }

        if (!updateState(free.serverId, ServerState.INGAME)) {
            logger.error("Failed to take server {} for match {}", free.serverId, match.id)
            return null
        }

        requested.remove(match.id)

        val assignment = Assignment(free.serverId, "${free.group.name}-${free.numericalId}")
        logger.info("Took server {} ({}) for match {}", assignment.serverName, assignment.serverId, match.id)
        return assignment
    }

    /**
     * Puts the server of a match that never made it to the transfer back to
     * available, so it can be handed to the next match.
     *
     * @param match the failed match.
     */
    suspend fun release(match: Match) {
        requested.remove(match.id)

        val assignment = match.assignment
        if (assignment == null) {
            logger.debug("Match {} had no server to release", match.id)
            return
        }

        if (updateState(assignment.serverId, ServerState.AVAILABLE)) {
            logger.info("Released server {} of match {}", assignment.serverName, match.id)
        } else {
            logger.error("Failed to release server {} of match {}", assignment.serverName, match.id)
        }
    }

    private suspend fun requestServer(match: Match, type: QueueType) {
        if (!requested.add(match.id)) {
            logger.debug("Match {} is still waiting for its requested server in group '{}'", match.id, type.group)
            return
        }

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
            logger.error("Failed to update server {} to state {}", serverId, state, e)
            false
        }
    }

}

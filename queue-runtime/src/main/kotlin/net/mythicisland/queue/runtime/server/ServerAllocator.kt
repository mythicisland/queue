package net.mythicisland.queue.runtime.server

import app.simplecloud.api.CloudApi
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import kotlinx.coroutines.future.await
import net.mythicisland.queue.shared.match.Assignment
import net.mythicisland.queue.shared.match.Match
import net.mythicisland.queue.shared.queue.QueueType
import org.apache.logging.log4j.LogManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Hands out simplecloud servers to matches.
 *
 * The reservations live here and not in a server property, so two matches can
 * never grab the same server. The property is only written as a marker, it
 * makes it easy to see on the cloud side which match a server belongs to.
 *
 * @param api the simplecloud api.
 */
class ServerAllocator(
    private val api: CloudApi,
) {

    private companion object {
        const val MATCH_PROPERTY = "match-id"
    }

    private val logger = LogManager.getLogger(ServerAllocator::class.java)

    private val reservations = ConcurrentHashMap<String, UUID>()
    private val requested = ConcurrentHashMap.newKeySet<UUID>()

    /**
     * Tries to reserve a free server for a match.
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
        val free = servers.firstOrNull { isFree(it) }

        if (free == null) {
            requestServer(match, type)
            return null
        }

        // Reserving with putIfAbsent keeps two matches from taking the same
        // server, even if they are allocated at the same time.
        val owner = reservations.putIfAbsent(free.serverId, match.id)
        if (owner != null && owner != match.id) {
            logger.debug("Server {} was taken by match {}, match {} keeps waiting", free.serverId, owner, match.id)
            return null
        }

        requested.remove(match.id)
        markServer(free, match)

        val assignment = Assignment(free.serverId, "${free.group.name}-${free.numericalId}")
        logger.info("Reserved server {} ({}) for match {}", assignment.serverName, assignment.serverId, match.id)
        return assignment
    }

    /**
     * Frees the server of a match that never made it to the transfer, so it
     * can be handed to the next match.
     *
     * @param match the failed match.
     */
    suspend fun release(match: Match) {
        requested.remove(match.id)

        val serverId = reservations.entries.firstOrNull { it.value == match.id }?.key
        if (serverId == null) {
            logger.debug("Match {} had no server to release", match.id)
            return
        }

        reservations.remove(serverId, match.id)
        clearServer(serverId)
        logger.info("Released server {} of match {}", serverId, match.id)
    }

    /**
     * Forgets the reservation of a finished match. The server is busy running
     * the game now, so the marker property stays for debugging.
     *
     * @param match the finished match.
     */
    fun forget(match: Match) {
        requested.remove(match.id)
        reservations.entries.removeAll { it.value == match.id }
    }

    /**
     * A server can be taken when it is idle, not reserved by another match and
     * not marked for one either.
     */
    private fun isFree(server: Server): Boolean {
        if (server.state != ServerState.AVAILABLE) return false
        if (reservations.containsKey(server.serverId)) return false

        val marker = server.properties[MATCH_PROPERTY] as? String
        return marker.isNullOrEmpty()
    }

    /**
     * Asks simplecloud for a new server, at most once per match.
     */
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
            // Allow another attempt on the next reconciliation.
            requested.remove(match.id)
            logger.error("Failed to request a server in group '{}' for match {}", type.group, match.id, e)
        }
    }

    private suspend fun markServer(server: Server, match: Match) {
        try {
            api.server().updateServerProperties(server.serverId, mapOf(MATCH_PROPERTY to match.id.toString())).await()
        } catch (e: Exception) {
            // Only a marker, the reservation above is what actually counts.
            logger.warn("Failed to mark server {} with match {}", server.serverId, match.id, e)
        }
    }

    private suspend fun clearServer(serverId: String) {
        try {
            api.server().updateServerProperties(serverId, mapOf(MATCH_PROPERTY to "")).await()
        } catch (e: Exception) {
            logger.warn("Failed to clear the match marker of server {}", serverId, e)
        }
    }

}

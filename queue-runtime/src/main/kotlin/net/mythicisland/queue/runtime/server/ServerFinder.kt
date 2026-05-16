package net.mythicisland.queue.runtime.server

import app.simplecloud.api.CloudApi
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import kotlinx.coroutines.future.await
import net.mythicisland.queue.shared.queue.Queue
import net.mythicisland.queue.runtime.repository.QueueTypeRepository
import org.apache.logging.log4j.LogManager

/**
 * Handles server discovery and reservation for queues.
 */
class ServerFinder(
    private val api: CloudApi,
    private val types: QueueTypeRepository,
) {

    private val logger = LogManager.getLogger(ServerFinder::class.java)

    /**
     * Finds the server currently assigned to the given queue.
     *
     * @param queue The queue to find a server for
     * @return The assigned server, or null if none found or queue type doesn't exist
     */
    suspend fun findServer(queue: Queue): Server? {
        val type = types.find(queue.type) ?: return null
        val servers = api.server().getServersByGroup(type.group).await()

        return servers.firstOrNull {
            it.properties["queue-id"] == queue.id.toString() && it.state == ServerState.AVAILABLE
        }
    }

    /**
     * Frees a server by clearing its queue id.
     *
     * @param server The server to free
     * @return true if the property was successfully removed, false on error
     */
    suspend fun freeServer(server: Server): Boolean {
        try {
            api.server().updateServerProperties(server.serverId, mapOf("queue-id" to "")).await()
            return true
        } catch (e: Exception) {
            logger.error("Failed to remove the queue-id property from server ${server.serverId}", e)
            return false
        }
    }

    /**
     * Attempts to reserve an available server, or requests a new one if none available.
     *
     * @param queue The queue to reserve or request a server for
     * @return The reserved server if one was available, null if a new server was requested
     */
    suspend fun reserveOrRequestServer(queue: Queue): Server? {
        val reserved = reserveServer(queue)
        if (reserved != null) {
            return reserved
        }

        requestNewServer(queue)
        return null
    }

    /**
     * Attempts to reserve an available server for the given queue.
     *
     * @param queue The queue to reserve a server for
     * @return The reserved server, or null if none available or queue type doesn't exist
     */
    private suspend fun reserveServer(queue: Queue): Server? {
        val type = types.find(queue.type) ?: return null
        val servers = api.server().getServersByGroup(type.group).await()
        val server = servers.firstOrNull {
            canReserveServer(queue, it)
        } ?: return null

        api.server().updateServerProperties(server.serverId, mapOf("queue-id" to queue.id.toString())).await()
        queue.server = server

        return server
    }

    /**
     * Checks if a server can be reserved for the given queue.
     *
     * A server can be reserved if:
     * - It is in [ServerState.AVAILABLE] state
     * - It belongs to the same group as the queue type
     * - It has no queue-id property, OR the property is empty, OR it already belongs to this queue
     *
     * @param queue The queue requesting the server
     * @param server The server to check
     * @return true if the server can be reserved, false otherwise
     */
    private fun canReserveServer(queue: Queue, server: Server): Boolean {
        val type = types.find(queue.type) ?: return false

        if (server.state != ServerState.AVAILABLE) return false
        if (type.group != server.group.name) return false

        val queueId = server.properties["queue-id"] as? String
        return queueId.isNullOrEmpty() || queueId == queue.id.toString()
    }

    /**
     * Reserves a specific server for the given queue.
     *
     * @param queue The queue to reserve the server for
     * @param server The specific server to reserve
     * @return true if the server was successfully reserved, false if it cannot be reserved
     */
    suspend fun reserveServer(queue: Queue, server: Server): Boolean {
        if (!canReserveServer(queue, server)) return false

        api.server().updateServerProperties(server.serverId, mapOf("queue-id" to queue.id.toString())).await()
        queue.server = server

        return true
    }

    /**
     * Queues a server start for a queue.
     *
     * @param queue The queue that needs a new server
     * @throws IllegalStateException if the queue type or group cannot be resolved
     */
    private suspend fun requestNewServer(queue: Queue) {
        val type = types.find(queue.type)
            ?: throw IllegalStateException("Queue type '${queue.type}' not found")

        val group = api.group().getGroupByName(type.group).await()
            ?: throw IllegalStateException("Group '${type.group}' not found for queue type '${queue.type}'")

        api.group().requestServerStart(group).await()
        logger.info("Requested server start for queue {} (group={})", queue.id, type.group)
    }
}
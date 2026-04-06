package net.mythicisland.queue.runtime.server

import app.simplecloud.api.CloudApi
import app.simplecloud.api.server.Server
import kotlinx.coroutines.future.await
import net.mythicisland.queue.shared.queue.Queue
import net.mythicisland.queue.runtime.repository.QueueTypeRepository
import org.apache.logging.log4j.LogManager

/**
 * Handles server discovery, reservation, and provisioning for queues.
 */
class ServerFinder(
    private val api: CloudApi,
    private val types: QueueTypeRepository,
) {

    private val logger = LogManager.getLogger(ServerFinder::class.java)

    /**
     * Finds the server currently assigned to the given queue.
     *
     * Searches for a server in the queue type's group that has the queue ID
     * stored in its "queue-id" property.
     *
     * @param queue The queue to find a server for
     * @return The assigned server, or null if none found or queue type doesn't exist
     */
    suspend fun findServer(queue: Queue): Server? {
        val type = types.find(queue.type) ?: return null

        // Get all servers in the queue type's group
        val servers = api.server().getServersByGroup(type.group).await()

        // Find the server with matching queue-id property
        return servers.firstOrNull {
            it.properties["queue-id"] == queue.id.toString()
        }
    }

    /**
     * Frees a server by clearing its queue assignment.
     *
     * Removes the "queue-id" property from the server, making it available
     * for other queues to reserve.
     *
     * @param server The server to free
     * @return true if the server was successfully freed, false on error
     */
    suspend fun freeServer(server: Server): Boolean {
        try {
            // Clear the queue-id property to mark server as available
            api.server().updateServerProperties(server.serverId, mapOf("queue-id" to "")).await()
            return true
        } catch (e: Exception) {
            logger.error("Failed to free server ${server.serverId}", e)
            return false
        }
    }

    /**
     * Attempts to reserve an available server, or requests a new one if none available.
     *
     * First tries to reserve an existing server from the pool. If none is available,
     * queues a start request via the SimpleCloud controller. The controller's reconciler
     * will start the server asynchronously; the queue transitions to WAITING_FOR_SERVER
     * until the server becomes AVAILABLE.
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
     * Attempts to reserve an available server from the pool for the given queue.
     *
     * Searches for a server in the queue type's group that can be reserved
     * (has no queue-id or matches this queue's ID), then assigns it to the queue.
     *
     * @param queue The queue to reserve a server for
     * @return The reserved server, or null if none available or queue type doesn't exist
     */
    private suspend fun reserveServer(queue: Queue): Server? {
        val type = types.find(queue.type) ?: return null

        // Get all servers in the queue type's group
        val servers = api.server().getServersByGroup(type.group).await()

        // Find first server that can be reserved
        val server = servers.firstOrNull {
            canReserveServer(queue, it)
        } ?: return null

        // Assign queue ID to the server
        api.server().updateServerProperties(server.serverId, mapOf("queue-id" to queue.id.toString())).await()
        queue.server = server

        return server
    }

    /**
     * Checks if a server can be reserved for the given queue.
     *
     * A server can be reserved if:
     * - It belongs to the same group as the queue type
     * - It has no queue-id property, OR the property is empty/null, OR it already belongs to this queue
     *
     * @param queue The queue requesting the server
     * @param server The server to check
     * @return true if the server can be reserved, false otherwise
     */
    private fun canReserveServer(queue: Queue, server: Server): Boolean {
        val type = types.find(queue.type) ?: return false

        // Check if server is in the correct group
        if (type.group != server.group.name) return false

        // Check if server is available (no queue-id) or already assigned to this queue
        val queueId = server.properties["queue-id"] as? String
        return queueId.isNullOrEmpty() || queueId == queue.id.toString()
    }

    /**
     * Reserves a specific server for the given queue.
     *
     * This overload allows reserving a specific server instance, rather than
     * searching for one. The server must pass the canReserveServer check.
     *
     * @param queue The queue to reserve the server for
     * @param server The specific server to reserve
     * @return true if the server was successfully reserved, false if it cannot be reserved
     */
    suspend fun reserveServer(queue: Queue, server: Server): Boolean {
        if (!canReserveServer(queue, server)) return false

        // Assign queue ID to the server
        api.server().updateServerProperties(server.serverId, mapOf("queue-id" to queue.id.toString())).await()
        queue.server = server

        return true
    }

    /**
     * Queues a server start request via the SimpleCloud controller.
     *
     * Resolves the queue type's group and submits a start request to the controller's
     * server start queue. The controller's reconciler processes the request on its next
     * tick and allocates a server, respecting the group's `max_servers` limit.
     *
     * The server is not immediately available — the queue should transition to
     * WAITING_FOR_SERVER and wait for the server registration event.
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
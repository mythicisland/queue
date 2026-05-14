package net.mythicisland.queue.runtime.reconciler

import app.simplecloud.api.CloudApi
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import build.buf.gen.mythicisland.queue.v1.QueueStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mythicisland.queue.shared.queue.Queue
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.repository.QueueRepository
import net.mythicisland.queue.runtime.repository.QueueTypeRepository
import net.mythicisland.queue.runtime.server.ServerFinder
import org.apache.logging.log4j.LogManager

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * Reconciles queue statuses based on queue updates or server registrations.
 */
class QueueReconciler(
    private val queues: QueueRepository,
    private val types: QueueTypeRepository,
    private val api: CloudApi,
    private val finder: ServerFinder,
    private val publisher: EventPublisher,
) {

    private val logger = LogManager.getLogger(QueueReconciler::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val mutex = ConcurrentHashMap<UUID, Mutex>()

    /**
     * Starts the Reconciler.
     */
    fun start() {
        logger.info("Starting up queue reconciler...")
        startPeriodicReconciliation()
        startCountdownReconciliation()
        startWaitingCountdownReconciliation()
        startServerRetryReconciliation()
        registerServerRegistrationSubscriber()
    }

    /**
     * Shutdowns the reconciler and cleanup resources.
     */
    fun shutdown() {
        logger.info("Shutting down queue reconciler...")
        scope.cancel()
    }

    /**
     * Reconciles a queue's status based on its current state.
     * Handles cascading transitions when a status change occurs immediately.
     *
     * @param queueId The ID of the queue to reconcile
     */
    suspend fun reconcile(queueId: UUID) {
        mutex.getOrPut(queueId) { Mutex() }.withLock {
            var previousStatus: QueueStatus

            do {
                val queue = queues.getQueue(queueId) ?: return

                if (queue.players.isEmpty() && queue.status != QueueStatus.FINISHED) {
                    logger.info("Queue {} has no players remaining, finishing", queue.id)
                    updateStatus(queue, QueueStatus.FINISHED)
                }

                previousStatus = queue.status

                when (queue.status) {
                    QueueStatus.NOT_ENOUGH_PLAYERS -> handleNotEnoughPlayers(queue)
                    QueueStatus.WAITING_COUNTDOWN -> handleWaitingForPlayersCountdown(queue)
                    QueueStatus.SEARCHING_SERVER -> handleSearchingServer(queue)
                    QueueStatus.WAITING_FOR_SERVER -> handleWaitingForServer(queue)
                    QueueStatus.SERVER_READY -> handleServerReady(queue)
                    QueueStatus.COUNTDOWN -> handleCountdown(queue)
                    QueueStatus.TELEPORTING -> handleTeleporting(queue)
                    QueueStatus.FINISHED -> handleFinished(queue)
                    else -> {
                        logger.warn("Queue {} has unhandled status {}, skipping reconciliation", queueId, queue.status)
                        return
                    }
                }

                queues.updateQueue(queue)
            } while (queue.status != previousStatus)
        }
    }

    /**
     * Updates the status of a queue and logs the transition.
     *
     * @param queue The queue to update
     * @param newStatus The new status
     */
    private fun updateStatus(queue: Queue, newStatus: QueueStatus) {
        val oldStatus = queue.status
        logger.info("Queue {} status: {} -> {}", queue.id, oldStatus, newStatus)
        queue.status = newStatus
        publisher.publishStatusUpdated(queue, oldStatus, newStatus)
    }

    /**
     * Handles a queue with NOT_ENOUGH_PLAYERS status.
     * Transitions to WAITING_COUNTDOWN when the minimum player capacity is reached.
     *
     * @param queue The Queue to handle
     */
    private fun handleNotEnoughPlayers(queue: Queue): Queue {
        val type = types.find(queue.type) ?: return queue

        if (queue.players.size >= type.minCapacity) {
            updateStatus(queue, QueueStatus.WAITING_COUNTDOWN)
            queue.waitingCountdownEndsAt = System.currentTimeMillis() + type.waitingCountdownSeconds * 1000
            logger.info("Queue {} waiting countdown started: {}s", queue.id, type.waitingCountdownSeconds)
        }

        return queue
    }

    /**
     * Handles a queue with WAITING_COUNTDOWN status.
     * Waits for more players while counting down. Transitions to SEARCHING_SERVER
     * when the countdown expires or the queue is full. Falls back to NOT_ENOUGH_PLAYERS
     * if players drop below minimum.
     *
     * @param queue The Queue to handle the waiting countdown
     */
    private fun handleWaitingForPlayersCountdown(queue: Queue): Queue {
        val type = types.find(queue.type) ?: return queue

        if (queue.players.size < type.minCapacity) {
            logger.info("Queue {} players dropped below minimum ({}/{}), resetting countdown", queue.id, queue.players.size, type.minCapacity)
            updateStatus(queue, QueueStatus.NOT_ENOUGH_PLAYERS)
            queue.waitingCountdownEndsAt = null
            return queue
        }

        if (queue.waitingCountdownRemaining <= 0 || queue.players.size >= type.maxCapacity) {
            val reason = if (queue.players.size >= type.maxCapacity) "queue full" else "countdown expired"
            logger.info("Queue {} waiting countdown finished ({}), searching server", queue.id, reason)
            updateStatus(queue, QueueStatus.SEARCHING_SERVER)
            queue.waitingCountdownEndsAt = null
        }

        return queue
    }

    /**
     * Handles a queue with SEARCHING_SERVER status.
     * Attempts to reserve an existing server or request a new one.
     * Transitions to SERVER_READY if a server is immediately available,
     * or WAITING_FOR_SERVER if a new server was requested.
     *
     * @param queue The Queue to handle server searching
     */
    private suspend fun handleSearchingServer(queue: Queue): Queue {
        logger.info("Queue {} searching for server (type: {})", queue.id, queue.type)

        try {
            val server = finder.reserveOrRequestServer(queue)

            if (server != null) {
                logger.info("Queue {} reserved server {}", queue.id, server.serverId)
                publisher.publishServerAssigned(queue, server.serverId)
                updateStatus(queue, QueueStatus.SERVER_READY)
            } else {
                logger.info("Queue {} requested new server, waiting for it to start", queue.id)
                updateStatus(queue, QueueStatus.WAITING_FOR_SERVER)
            }
        } catch (e: Exception) {
            logger.error("Queue {} failed to find/reserve server", queue.id, e)
            updateStatus(queue, QueueStatus.WAITING_FOR_SERVER)
        }

        return queue
    }

    /**
     * Handles a queue with WAITING_FOR_SERVER status.
     * Checks if a server has become available for the queue, either through
     * direct assignment or by searching for one.
     *
     * @param queue The Queue to handle waiting for a server
     */
    private suspend fun handleWaitingForServer(queue: Queue): Queue {
        if (queue.server != null) {
            logger.info("Queue {} server already assigned ({}), marking ready", queue.id, queue.server?.serverId)
            updateStatus(queue, QueueStatus.SERVER_READY)
            return queue
        }

        val server = finder.findServer(queue)
        if (server != null) {
            logger.info("Queue {} found available server {}", queue.id, server.serverId)
            queue.server = server
            publisher.publishServerAssigned(queue, server.serverId)
            updateStatus(queue, QueueStatus.SERVER_READY)
        }

        return queue
    }

    /**
     * Handles a queue with SERVER_READY status.
     * Initializes the game countdown and transitions to COUNTDOWN.
     *
     * @param queue The Queue to handle server ready
     */
    private fun handleServerReady(queue: Queue): Queue {
        val type = types.find(queue.type) ?: return queue

        updateStatus(queue, QueueStatus.COUNTDOWN)
        queue.countdownEndsAt = System.currentTimeMillis() + type.countdownSeconds * 1000
        logger.info("Queue {} game countdown started: {}s on server {}", queue.id, type.countdownSeconds, queue.server?.serverId)

        return queue
    }

    /**
     * Handles a queue with COUNTDOWN status.
     * Transitions to TELEPORTING when the countdown expires.
     *
     * @param queue The Queue to handle the countdown
     */
    private fun handleCountdown(queue: Queue): Queue {
        if (queue.countdownRemaining <= 0) {
            logger.info("Queue {} game countdown finished, teleporting players", queue.id)
            updateStatus(queue, QueueStatus.TELEPORTING)
            queue.countdownEndsAt = null
        }

        return queue
    }

    /**
     * Handles a queue with TELEPORTING status.
     * Transfers all players to the assigned server and transitions to FINISHED.
     *
     * @param queue The Queue to handle player teleporting
     */
    private suspend fun handleTeleporting(queue: Queue): Queue {
        val server = queue.server ?: run {
            logger.error("Queue {} in TELEPORTING but no server assigned, searching again", queue.id)
            updateStatus(queue, QueueStatus.SEARCHING_SERVER)
            return queue
        }

        val serverName = "${server.group.name}-${server.numericalId}"
        logger.info("Queue {} teleporting {} players to server {} ({})", queue.id, queue.players.size, serverName, server.serverId)

        val transferredPlayers = mutableListOf<UUID>()

        queue.players.toList().forEach { playerId ->
            try {
                val player = api.player().get(playerId).await()
                if (player == null) {
                    logger.warn("Queue {} player {} is offline, skipping teleport", queue.id, playerId)
                    return@forEach
                }

                val result = player.connect(serverName).await()
                logger.info("Queue {} player {} connect result: {}", queue.id, playerId, result)
                transferredPlayers.add(playerId)
            } catch (e: Exception) {
                logger.error("Failed to teleport player {} to server {}", playerId, server.serverId, e)
            }
        }

        publisher.publishTransfer(queue, server.serverId, transferredPlayers)
        updateStatus(queue, QueueStatus.FINISHED)
        return queue
    }

    /**
     * Handles a queue with FINISHED status.
     * Frees the assigned server and deletes the queue.
     *
     * @param queue The Queue to finish
     */
    private suspend fun handleFinished(queue: Queue): Queue {
        logger.info("Queue {} finished, cleaning up (players={}, server={})", queue.id, queue.players.size, queue.server?.serverId)

        queue.server?.let { server ->
            logger.info("Queue {} freeing server {}", queue.id, server.serverId)
            finder.freeServer(server)
        }

        queues.deleteQueue(queue.id)
        return queue
    }

    /**
     * Reconciles all queues in the repository.
     */
    private suspend fun reconcileAll() {
        queues.getAllQueues().forEach { queue ->
            reconcile(queue.id)
        }
    }

    /**
     * Handles server registration events.
     * Checks if any WAITING_FOR_SERVER queues can use the new server.
     *
     * @param server The newly registered server
     */
    private suspend fun handleServerRegistration(server: Server) {
        logger.info("Server {} became available, checking waiting queues", server.serverId)
        val waitingQueues = queues.getAllQueues().filter { it.status == QueueStatus.WAITING_FOR_SERVER }

        if (waitingQueues.isEmpty()) {
            logger.debug("No queues waiting for a server, freeing server {}", server.serverId)
            finder.freeServer(server)
            return
        }

        for (queue in waitingQueues) {
            if (finder.reserveServer(queue, server)) {
                logger.info("Server {} assigned to waiting queue {} (type={})", server.serverId, queue.id, queue.type)
                reconcile(queue.id)
                return
            }
        }

        logger.debug("Server {} does not match any waiting queue, freeing", server.serverId)
        finder.freeServer(server)
    }

    /**
     * Registers a listener for server state changes.
     * When a server becomes AVAILABLE, checks if waiting queues can use it.
     */
    fun registerServerRegistrationSubscriber() {
        api.event().server().onStateChanged { event ->
            val server = event.server ?: return@onStateChanged
            if (server.serverBase?.type != GroupServerType.SERVER) return@onStateChanged
            if (event.newState == ServerState.AVAILABLE && event.oldState != ServerState.AVAILABLE) {
                scope.launch {
                    handleServerRegistration(event.server)
                }
            }
        }
    }

    /**
     * Clears all reconciliation state for a queue.
     *
     * @param id The queue ID to clear state for
     */
    fun clear(id: UUID) {
        mutex.remove(id)
    }

    /**
     * Periodically ticks queues in WAITING_COUNTDOWN status every 500ms.
     */
    private fun startWaitingCountdownReconciliation() {
        scope.launch {
            while (isActive) {
                delay(500.milliseconds)
                queues.getAllQueues()
                    .filter { it.status == QueueStatus.WAITING_COUNTDOWN }
                    .forEach { reconcile(it.id) }
            }
        }
    }

    /**
     * Periodically ticks queues in COUNTDOWN status every 500ms.
     */
    private fun startCountdownReconciliation() {
        scope.launch {
            while (isActive) {
                delay(500.milliseconds)
                queues.getAllQueues()
                    .filter { it.status == QueueStatus.COUNTDOWN }
                    .forEach { reconcile(it.id) }
            }
        }
    }

    /**
     * Periodically retries server reservation for queues stuck in WAITING_FOR_SERVER every 5 seconds.
     */
    private fun startServerRetryReconciliation() {
        scope.launch {
            while (isActive) {
                delay(5000.milliseconds)
                queues.getAllQueues()
                    .filter { it.status == QueueStatus.WAITING_FOR_SERVER }
                    .forEach { reconcile(it.id) }
            }
        }
    }

    /**
     * Periodically reconciles all queues every 30 seconds.
     */
    private fun startPeriodicReconciliation() {
        scope.launch {
            while (isActive) {
                reconcileAll()
                delay(30000.milliseconds)
            }
        }
    }

}

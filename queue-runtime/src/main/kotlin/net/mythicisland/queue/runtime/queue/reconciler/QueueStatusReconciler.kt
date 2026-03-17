package net.mythicisland.queue.runtime.queue.reconciler

import app.simplecloud.api.event.EventApi
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.player.PlayerApi
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import build.buf.gen.mythicisland.queue.v1.QueueStatus
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mythicisland.queue.runtime.extension.asPlayerOrNull
import net.mythicisland.queue.runtime.queue.Queue
import net.mythicisland.queue.runtime.queue.event.EventPublisher
import net.mythicisland.queue.runtime.queue.repository.QueueRepository
import net.mythicisland.queue.runtime.queue.repository.QueueTypeRepository
import net.mythicisland.queue.runtime.queue.server.ServerFinder
import net.mythicisland.queue.runtime.queue.visualizer.QueueVisualizer
import org.apache.logging.log4j.LogManager

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Reconciles queue statuses based on queue updates or server registrations.
 *
 * This is the core lifecycle manager for queues. It handles status transitions
 * from [QueueStatus.NOT_ENOUGH_PLAYERS] through to [QueueStatus.FINISHED],
 * managing countdowns, server discovery, and player transfers.
 */
class QueueStatusReconciler(
    private val queues: QueueRepository,
    private val types: QueueTypeRepository,
    private val eventApi: EventApi,
    private val playerApi: PlayerApi,
    private val finder: ServerFinder,
    private val visualizer: QueueVisualizer,
    private val publisher: EventPublisher,
) {

    private val logger = LogManager.getLogger(QueueStatusReconciler::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val lastWaitingTick = ConcurrentHashMap<UUID, Long>()
    private val lastCountdownTick = ConcurrentHashMap<UUID, Long>()
    private val queueMutexes = ConcurrentHashMap<UUID, Mutex>()

    private fun getMutex(queueId: UUID): Mutex = queueMutexes.getOrPut(queueId) { Mutex() }

    /**
     * Reconciles a queue's status based on its current state.
     * Handles cascading transitions when a status change occurs immediately.
     *
     * @param queueId The ID of the queue to reconcile
     */
    suspend fun reconcile(queueId: UUID) {
        getMutex(queueId).withLock {
            reconcileInternal(queueId)
        }
    }

    private suspend fun reconcileInternal(queueId: UUID) {
        var previousStatus: QueueStatus

        do {
            val queue = queues.getQueue(queueId) ?: return
            val type = types.find(queue.type) ?: return

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
                else -> return
            }

            queues.updateQueue(queue)

            if (queue.status != QueueStatus.FINISHED) {
                visualizer.send(queue, type, queue.status)
            }
        } while (queue.status != previousStatus)
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
            queue.waitingCountdownRemaining = type.waitingCountdownSeconds * 1000
            lastWaitingTick[queue.id] = System.currentTimeMillis()
            logger.info("Queue {} waiting countdown started: {}s", queue.id, type.waitingCountdownSeconds)
        }

        return queue
    }

    /**
     * Decrements the waiting countdown using delta time.
     *
     * @param queue The Queue to update the waiting countdown
     */
    private fun updateWaitingCountdown(queue: Queue) {
        val now = System.currentTimeMillis()
        val lastTick = lastWaitingTick.getOrPut(queue.id) { now }
        val delta = now - lastTick
        queue.waitingCountdownRemaining = (queue.waitingCountdownRemaining - delta).coerceAtLeast(0)
        lastWaitingTick[queue.id] = now
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

        updateWaitingCountdown(queue)

        if (queue.players.size < type.minCapacity) {
            logger.info("Queue {} players dropped below minimum ({}/{}), resetting countdown", queue.id, queue.players.size, type.minCapacity)
            updateStatus(queue, QueueStatus.NOT_ENOUGH_PLAYERS)
            queue.waitingCountdownRemaining = 0
            lastWaitingTick.remove(queue.id)
            return queue
        }

        if (queue.waitingCountdownRemaining <= 0 || queue.players.size >= type.maxCapacity) {
            val reason = if (queue.players.size >= type.maxCapacity) "queue full" else "countdown expired"
            logger.info("Queue {} waiting countdown finished ({}), searching server", queue.id, reason)
            updateStatus(queue, QueueStatus.SEARCHING_SERVER)
            lastWaitingTick.remove(queue.id)
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
                logger.info("Queue {} no server available, waiting for new server", queue.id)
                updateStatus(queue, QueueStatus.WAITING_FOR_SERVER)
            }
        } catch (_: NotImplementedError) {
            logger.warn("Queue {} server provisioning not yet available, waiting for existing server", queue.id)
            updateStatus(queue, QueueStatus.WAITING_FOR_SERVER)
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
        queue.countdownRemaining = type.countdownSeconds * 1000
        lastCountdownTick[queue.id] = System.currentTimeMillis()
        logger.info("Queue {} game countdown started: {}s on server {}", queue.id, type.countdownSeconds, queue.server?.serverId)

        return queue
    }

    /**
     * Decrements the game countdown using delta time.
     *
     * @param queue The Queue to update the countdown
     */
    private fun updateCountdown(queue: Queue) {
        val now = System.currentTimeMillis()
        val lastTick = lastCountdownTick.getOrPut(queue.id) { now }
        val delta = now - lastTick
        queue.countdownRemaining = (queue.countdownRemaining - delta).coerceAtLeast(0)
        lastCountdownTick[queue.id] = now
    }

    /**
     * Handles a queue with COUNTDOWN status.
     * Decrements the timer and transitions to TELEPORTING when done.
     *
     * @param queue The Queue to handle the countdown
     */
    private fun handleCountdown(queue: Queue): Queue {
        updateCountdown(queue)

        if (queue.countdownRemaining <= 0) {
            logger.info("Queue {} game countdown finished, teleporting players", queue.id)
            updateStatus(queue, QueueStatus.TELEPORTING)
            lastCountdownTick.remove(queue.id)
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
                val player = playerId.asPlayerOrNull(playerApi)
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
     * Called periodically as a safety net to ensure all queues are in the correct status.
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
     * Registers a subscriber for server state change events.
     * When a server becomes AVAILABLE, checks if waiting queues can use it.
     */
    fun registerServerRegistrationSubscriber() {
        eventApi.server().onStateChanged { event ->
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
     * Cancels all reconciliation loops and cleans up resources.
     * Called during runtime shutdown to stop background processing.
     */
    fun shutdown() {
        logger.info("Shutting down reconciler...")
        scope.cancel()
    }

    /**
     * Clears all reconciliation state for a queue.
     *
     * @param id The queue ID to clear state for
     */
    fun clear(id: UUID) {
        lastWaitingTick.remove(id)
        lastCountdownTick.remove(id)
        queueMutexes.remove(id)
    }

    /**
     * Periodically ticks queues in WAITING_COUNTDOWN status every 500ms.
     */
    fun startWaitingCountdownReconciliation() {
        scope.launch {
            while (true) {
                delay(500)
                queues.getAllQueues()
                    .filter { it.status == QueueStatus.WAITING_COUNTDOWN }
                    .forEach { reconcile(it.id) }
            }
        }
    }

    /**
     * Periodically ticks queues in COUNTDOWN status every 500ms.
     */
    fun startCountdownReconciliation() {
        scope.launch {
            while (true) {
                delay(500)
                queues.getAllQueues()
                    .filter { it.status == QueueStatus.COUNTDOWN }
                    .forEach { reconcile(it.id) }
            }
        }
    }

    /**
     * Periodically sends actionbar to all players in active queues every second.
     * Minecraft actionbars fade after ~2 seconds, so continuous sending is required.
     */
    fun startVisualizerLoop() {
        scope.launch {
            while (true) {
                delay(1000)
                queues.getAllQueues()
                    .filter { it.status != QueueStatus.FINISHED }
                    .forEach { queue ->
                        val type = types.find(queue.type) ?: return@forEach
                        try {
                            visualizer.send(queue, type, queue.status)
                        } catch (e: Exception) {
                            logger.debug("Failed to send visualizer for queue {}: {}", queue.id, e.message)
                        }
                    }
            }
        }
    }

    /**
     * Periodically retries server reservation for queues stuck in WAITING_FOR_SERVER every 5 seconds.
     * Complements the event-driven approach from [registerServerRegistrationSubscriber] with
     * active polling to recover from missed events or transient failures.
     */
    fun startServerRetryReconciliation() {
        scope.launch {
            while (true) {
                delay(5000)
                queues.getAllQueues()
                    .filter { it.status == QueueStatus.WAITING_FOR_SERVER }
                    .forEach { reconcile(it.id) }
            }
        }
    }

    /**
     * Periodically reconciles all queues every 30 seconds as a safety net.
     */
    fun startPeriodicReconciliation() {
        scope.launch {
            while (true) {
                reconcileAll()
                delay(30000)
            }
        }
    }

}

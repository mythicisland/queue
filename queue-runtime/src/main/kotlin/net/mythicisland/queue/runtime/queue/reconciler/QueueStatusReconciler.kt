package net.mythicisland.queue.runtime.queue.reconciler

import app.simplecloud.api.event.EventApi
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.player.PlayerApi
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import build.buf.gen.mythicisland.queue.v1.QueueStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mythicisland.queue.runtime.queue.Queue
import net.mythicisland.queue.runtime.queue.repository.QueueRepository
import net.mythicisland.queue.runtime.queue.repository.QueueTypeRepository
import net.mythicisland.queue.runtime.queue.server.ServerFinder
import net.mythicisland.queue.runtime.queue.visualizer.QueueVisualizer
import java.util.UUID

/**
 * Reconciles queue statuses based on queue updates or server registrations.
 */
class QueueStatusReconciler(
    private val queues: QueueRepository,
    private val types: QueueTypeRepository,
    private val eventApi: EventApi,
    private val playerApi: PlayerApi,
    private val finder: ServerFinder,
    private val visualizer: QueueVisualizer,
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Reconciles a queue's status based on its status state.
     * This method should be called when a queue is updated or a server is registered.
     *
     * @param queueId The ID of the queue to reconcile
     */
    suspend fun reconcile(queueId: UUID) {

    }

    /**
     * Updates the status of a queue.
     *
     * @param queueId The ID of the queue to update the status
     * @param newStatus The new status of the queue
     */
    private fun updateStatus(queueId: UUID, newStatus: QueueStatus) {

    }

    /**
     * Handles a queue with NOT_ENOUGH_PLAYERS status.
     * Checks if there are enough players to start the player waiting countdown.
     *
     * @param queue The Queue to handle not enough players
     */
    private suspend fun handleNotEnoughPlayers(queue: Queue): Queue {
        return queue
    }

    /**
     * Updates the waiting countdown for a queue using delta time.
     * Returns the remaining time in milliseconds.
     *
     * @param queue The Queue to update the waiting countdown
     */
    private fun updateWaitingCountdown(queue: Queue) {
    }

    /**
     * Handles a queue with WAITING_COUNTDOWN status.
     * Updates the countdown timer and transitions to SEARCHING_SERVER when done.
     *
     * @param queue The Queue to handle the waiting countdown
     */
    private suspend fun handleWaitingForPlayersCountdown(queue: Queue): Queue {
        return queue
    }

    /**
     * Handles a queue with SEARCHING_SERVER status.
     * Attempts to find or request a server for the queue.
     *
     * @param queue The Queue to handle server searching
     */
    private suspend fun handleSearchingServer(queue: Queue): Queue {
        return queue
    }

    /**
     * Handles a queue with WAITING_FOR_SERVER status.
     * Checks if a server has become available for the queue.
     *
     * @param queue The Queue to handle waiting for a server
     */
    private suspend fun handleWaitingForServer(queue: Queue): Queue {
        return queue
    }

    /**
     * Handles a queue with SERVER_READY status.
     * Starts the countdown for teleporting players.
     *
     * @param queue The Queue to handle server ready
     */
    private suspend fun handleServerReady(queue: Queue): Queue {
        return queue
    }

    /**
     * Updates the countdown for a queue using delta time.
     * Returns the remaining time in milliseconds.
     *
     * @param queue The Queue to update the countdown
     */
    private fun updateCountdown(queue: Queue) {
    }

    /**
     * Handles a queue with COUNTDOWN status.
     * Updates the countdown timer and transitions to TELEPORTING when done.
     *
     * @param queue The Queue to handle the countdown
     */
    private suspend fun handleCountdown(queue: Queue): Queue {
        return queue
    }

    /**
     * Handles a queue with TELEPORTING status.
     * Teleports players to the server and marks the queue as finished.
     *
     * @param queue The Queue to handle player teleporting
     */
    private suspend fun handleTeleporting(queue: Queue): Queue {
        return queue
    }

    /**
     * Handles a queue with FINISHED status.
     * Cleans up the queue.
     *
     * @param queue The Queue to finish
     */
    private suspend fun handleFinished(queue: Queue): Queue {
        return queue
    }

    /**
     * Reconciles all queues in the repository.
     * This can be called periodically to ensure all queues are in the correct status.
     */
    suspend fun reconcileAll() {
        /*queues.getAllQueues().forEach { queue ->
            reconcile(queue.id)
        }*/
    }

    /**
     * Handles server registration events.
     * Checks if any waiting queues can use the new server.
     *
     * @param server The newly registered server
     */
    suspend fun handleServerRegistration(server: Server) {
        /*this@QueueStatusReconciler.queues.getAllQueues()
            .filter { queueStatuses[it.id] == Status.WAITING_FOR_SERVER }.forEach { queue ->
                if (finder.reserveServer(queue, server)) {
                    updateStatus(queue.id, Status.SERVER_READY)
                    reconcile(queue.id)
                    return
                }
            }
        // Free the server as we found no queues that match this server
        finder.freeServer(server)*/
    }

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

    fun clear(id: UUID) {
    }

    fun startWaitingCountdownReconciliation() {
        scope.launch {
            while (true) {
                delay(500)
            }
        }
    }


    fun startCountdownReconciliation() {
        scope.launch {
            while (true) {
                delay(500)
            }
        }
    }

    fun startPeriodicReconciliation() {
        scope.launch {
            while (true) {
                reconcileAll()
                delay(30000) // 30 seconds
            }
        }
    }


}
package net.mythicisland.queue.runtime.event

import build.buf.gen.mythicisland.queue.v1.DequeueEvent
import build.buf.gen.mythicisland.queue.v1.EnqueueEvent
import build.buf.gen.mythicisland.queue.v1.QueueCreatedEvent
import build.buf.gen.mythicisland.queue.v1.QueueDeletedEvent
import build.buf.gen.mythicisland.queue.v1.QueueServerAssignedEvent
import build.buf.gen.mythicisland.queue.v1.QueueStatusUpdatedEvent
import build.buf.gen.mythicisland.queue.v1.QueueTransferEvent
import build.buf.gen.mythicisland.queue.v1.QueueUpdatedEvent
import build.buf.gen.mythicisland.queue.v1.QueueStatus
import com.google.protobuf.MessageLite
import io.nats.client.Connection
import net.mythicisland.queue.shared.event.QueueEventNames
import net.mythicisland.queue.shared.queue.Queue
import org.apache.logging.log4j.LogManager
import java.util.UUID

/**
 * Publishes queue lifecycle events to NATS.
 */
class EventPublisher(
    private val connection: Connection,
) {

    private val logger = LogManager.getLogger(EventPublisher::class.java)

    /**
     * Publishes an [EnqueueEvent] when players join a queue.
     *
     * @param queue The queue that players joined
     * @param playerIds The UUIDs of the players that were enqueued
     */
    fun publishEnqueue(queue: Queue, playerIds: List<UUID>) {
        val event = EnqueueEvent.newBuilder()
            .setQueue(queue.toDefinition())
            .addAllPlayerIds(playerIds.map { it.toString() })
            .build()

        publish(QueueEventNames.ENQUEUE, event)
    }

    /**
     * Publishes a [DequeueEvent] when players leave a queue.
     *
     * @param queue The queue that players left
     * @param playerIds The UUIDs of the players that were dequeued
     */
    fun publishDequeue(queue: Queue, playerIds: List<UUID>) {
        val event = DequeueEvent.newBuilder()
            .setQueue(queue.toDefinition())
            .addAllPlayerIds(playerIds.map { it.toString() })
            .build()

        publish(QueueEventNames.DEQUEUE, event)
    }

    /**
     * Publishes a [QueueCreatedEvent] when a new queue is created.
     *
     * @param queue The newly created queue
     */
    fun publishQueueCreated(queue: Queue) {
        val event = QueueCreatedEvent.newBuilder()
            .setQueue(queue.toDefinition())
            .build()

        publish(QueueEventNames.QUEUE_CREATED, event)
    }

    /**
     * Publishes a [QueueUpdatedEvent] when a queue's state changes.
     *
     * @param before The queue's protobuf snapshot before the change
     * @param after The queue's protobuf snapshot after the change
     */
    fun publishQueueUpdated(
        before: build.buf.gen.mythicisland.queue.v1.Queue,
        after: build.buf.gen.mythicisland.queue.v1.Queue,
    ) {
        val event = QueueUpdatedEvent.newBuilder()
            .setBefore(before)
            .setAfter(after)
            .build()

        publish(QueueEventNames.QUEUE_UPDATED, event)
    }

    /**
     * Publishes a [QueueStatusUpdatedEvent] when a queue transitions between statuses.
     *
     * @param queue The queue whose status changed
     * @param oldStatus The previous status
     * @param newStatus The new status
     */
    fun publishStatusUpdated(queue: Queue, oldStatus: QueueStatus, newStatus: QueueStatus) {
        val event = QueueStatusUpdatedEvent.newBuilder()
            .setQueue(queue.toDefinition())
            .setOldStatus(oldStatus)
            .setNewStatus(newStatus)
            .build()

        publish(QueueEventNames.QUEUE_STATUS_UPDATED, event)
    }

    /**
     * Publishes a [QueueDeletedEvent] when a queue is removed.
     *
     * @param queue The queue that was deleted
     */
    fun publishQueueDeleted(queue: Queue) {
        val event = QueueDeletedEvent.newBuilder()
            .setQueue(queue.toDefinition())
            .build()

        publish(QueueEventNames.QUEUE_DELETED, event)
    }

    /**
     * Publishes a [QueueServerAssignedEvent] when a server is reserved for a queue.
     *
     * @param queue The queue that received a server
     * @param serverId The ID of the assigned server
     */
    fun publishServerAssigned(queue: Queue, serverId: String) {
        val event = QueueServerAssignedEvent.newBuilder()
            .setQueue(queue.toDefinition())
            .setServerId(serverId)
            .build()

        publish(QueueEventNames.QUEUE_SERVER_ASSIGNED, event)
    }

    /**
     * Publishes a [QueueTransferEvent] when players are teleported to a game server.
     *
     * @param queue The queue whose players were transferred
     * @param serverId The ID of the target server
     * @param playerIds The UUIDs of the transferred players
     */
    fun publishTransfer(queue: Queue, serverId: String, playerIds: List<UUID>) {
        val event = QueueTransferEvent.newBuilder()
            .setQueue(queue.toDefinition())
            .setServerId(serverId)
            .addAllPlayerIds(playerIds.map { it.toString() })
            .build()

        publish(QueueEventNames.QUEUE_TRANSFER, event)
    }

    /**
     * Publishes a protobuf message to the given NATS subject.
     *
     * @param subject The NATS subject to publish to
     * @param message The protobuf message to serialize and publish
     */
    private fun publish(subject: String, message: MessageLite) {
        try {
            connection.publish(subject, message.toByteArray())
            logger.debug("Published event to {}", subject)
        } catch (e: Exception) {
            logger.error("Failed to publish event to {}", subject, e)
        }
    }
}
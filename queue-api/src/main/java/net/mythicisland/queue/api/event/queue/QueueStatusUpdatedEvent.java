package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when a queue transitions from one lifecycle status to another.
 */
public interface QueueStatusUpdatedEvent {

    /**
     * Gets the unique identifier of the queue.
     *
     * @return the unique ID of the queue
     */
    UUID queueId();

    /**
     * Gets the name of the queue type.
     *
     * @return the queue type name
     */
    String queueType();

    /**
     * Gets the list of players currently in the queue.
     *
     * @return the player UUIDs in the queue
     */
    List<UUID> queuePlayerIds();

    /**
     * Gets the status of the queue before the update.
     *
     * @return the status before the transition
     */
    QueueStatus oldStatus();

    /**
     * Gets the new status of the queue.
     *
     * @return the status after the transition
     */
    QueueStatus newStatus();
}

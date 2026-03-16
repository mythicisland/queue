package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Fired when a queue transitions from one status to another.
 */
public interface QueueStatusUpdatedEvent {

    /**
     * @return the unique ID of the queue
     */
    UUID queueId();

    /**
     * @return the queue type name
     */
    String queueType();

    /**
     * @return the player UUIDs in the queue
     */
    List<UUID> queuePlayerIds();

    /**
     * @return the status before the transition
     */
    QueueStatus oldStatus();

    /**
     * @return the status after the transition
     */
    QueueStatus newStatus();
}

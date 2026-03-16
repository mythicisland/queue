package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Fired when a new queue instance is created.
 */
public interface QueueCreatedEvent {

    /**
     * @return the unique ID of the created queue
     */
    UUID queueId();

    /**
     * @return the queue type name
     */
    String queueType();

    /**
     * @return the queue's initial status
     */
    QueueStatus queueStatus();

    /**
     * @return the player UUIDs in the queue at creation time
     */
    List<UUID> queuePlayerIds();
}

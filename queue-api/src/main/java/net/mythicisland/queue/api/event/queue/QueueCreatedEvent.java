package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when a new queue instance is created.
 */
public interface QueueCreatedEvent {

    /**
     * Gets the unique identifier of the created queue.
     *
     * @return the unique ID of the created queue
     */
    UUID queueId();

    /**
     * Gets the name of the queue type (e.g., "skyblock").
     *
     * @return the queue type name
     */
    String queueType();

    /**
     * Gets the initial status assigned to the queue.
     *
     * @return the queue's initial status
     */
    QueueStatus queueStatus();

    /**
     * Gets the list of players that were in the queue at the moment of creation.
     *
     * @return the player UUIDs in the queue at creation time
     */
    List<UUID> queuePlayerIds();
}

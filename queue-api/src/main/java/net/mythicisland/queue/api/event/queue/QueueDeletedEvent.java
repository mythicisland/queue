package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when a queue instance is deleted, typically after it has finished its lifecycle.
 */
public interface QueueDeletedEvent {

    /**
     * Gets the unique identifier of the deleted queue.
     *
     * @return the unique ID of the deleted queue
     */
    UUID queueId();

    /**
     * Gets the name of the queue type.
     *
     * @return the queue type name
     */
    String queueType();

    /**
     * Gets the final status of the queue before it was deleted.
     *
     * @return the queue's final status
     */
    QueueStatus queueStatus();

    /**
     * Gets the list of players that were in the queue at the time of deletion.
     *
     * @return the player UUIDs that were in the queue at deletion time
     */
    List<UUID> queuePlayerIds();
}

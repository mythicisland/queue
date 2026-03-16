package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Fired when a queue instance is deleted after finishing its lifecycle.
 */
public interface QueueDeletedEvent {

    /**
     * @return the unique ID of the deleted queue
     */
    UUID queueId();

    /**
     * @return the queue type name
     */
    String queueType();

    /**
     * @return the queue's final status
     */
    QueueStatus queueStatus();

    /**
     * @return the player UUIDs that were in the queue at deletion time
     */
    List<UUID> queuePlayerIds();
}

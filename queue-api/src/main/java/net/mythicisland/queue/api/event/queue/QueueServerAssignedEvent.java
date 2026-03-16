package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Fired when a server is reserved and assigned to a queue.
 */
public interface QueueServerAssignedEvent {

    /**
     * @return the unique ID of the queue
     */
    UUID queueId();

    /**
     * @return the queue type name
     */
    String queueType();

    /**
     * @return the queue's current status
     */
    QueueStatus queueStatus();

    /**
     * @return the player UUIDs in the queue
     */
    List<UUID> queuePlayerIds();

    /**
     * @return the ID of the assigned server
     */
    String serverId();
}

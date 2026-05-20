package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when a game server is reserved and assigned to a specific queue.
 */
public interface QueueServerAssignedEvent {

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
     * Gets the current status of the queue.
     *
     * @return the queue's current status
     */
    QueueStatus queueStatus();

    /**
     * Gets the list of players currently in the queue.
     *
     * @return the player UUIDs in the queue
     */
    List<UUID> queuePlayerIds();

    /**
     * Gets the unique identifier of the assigned game server.
     *
     * @return the ID of the assigned server
     */
    String serverId();
}

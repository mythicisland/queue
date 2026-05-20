package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when players are being transferred to a game server.
 */
public interface QueueTransferEvent {

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
     * Gets the list of all player UUIDs currently in the queue.
     *
     * @return all player UUIDs in the queue
     */
    List<UUID> queuePlayerIds();

    /**
     * Gets the unique identifier of the target game server.
     *
     * @return the ID of the target server
     */
    String serverId();

    /**
     * Gets the list of UUIDs of the players that are being transferred.
     *
     * @return the UUIDs of the players that were successfully transferred
     */
    List<UUID> transferredPlayerIds();
}

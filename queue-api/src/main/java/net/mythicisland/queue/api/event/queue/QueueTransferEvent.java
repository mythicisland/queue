package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Fired when players are transferred to a game server.
 */
public interface QueueTransferEvent {

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
     * @return all player UUIDs in the queue
     */
    List<UUID> queuePlayerIds();

    /**
     * @return the ID of the target server
     */
    String serverId();

    /**
     * @return the UUIDs of the players that were successfully transferred
     */
    List<UUID> transferredPlayerIds();
}

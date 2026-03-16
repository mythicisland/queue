package net.mythicisland.queue.api.event.player;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Fired when one or more players are removed from a queue.
 */
public interface DequeueEvent {

    /**
     * @return the unique ID of the queue the players left
     */
    UUID queueId();

    /**
     * @return the queue type name
     */
    String queueType();

    /**
     * @return the queue's status after the dequeue
     */
    QueueStatus queueStatus();

    /**
     * @return all player UUIDs remaining in the queue
     */
    List<UUID> queuePlayerIds();

    /**
     * @return the UUIDs of the players that were dequeued
     */
    List<UUID> playerIds();
}

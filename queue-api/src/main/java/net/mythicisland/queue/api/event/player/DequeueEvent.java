package net.mythicisland.queue.api.event.player;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when one or more players are removed from a queue.
 */
public interface DequeueEvent {

    /**
     * Gets the unique identifier of the queue the players left.
     *
     * @return the unique ID of the queue the players left
     */
    UUID queueId();

    /**
     * Gets the name of the queue type.
     *
     * @return the queue type name
     */
    String queueType();

    /**
     * Gets the status of the queue after the players were removed.
     *
     * @return the queue's status after the dequeue
     */
    QueueStatus queueStatus();

    /**
     * Gets the list of all player UUIDs remaining in the queue.
     *
     * @return all player UUIDs remaining in the queue
     */
    List<UUID> queuePlayerIds();

    /**
     * Gets the list of UUIDs of the players that were just removed from the queue.
     *
     * @return the UUIDs of the players that were dequeued
     */
    List<UUID> playerIds();
}

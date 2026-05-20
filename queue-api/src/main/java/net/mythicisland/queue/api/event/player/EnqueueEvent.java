package net.mythicisland.queue.api.event.player;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when one or more players are added to a queue.
 */
public interface EnqueueEvent {

    /**
     * Gets the unique identifier of the queue the players joined.
     *
     * @return the unique ID of the queue the players joined
     */
    UUID queueId();

    /**
     * Gets the name of the queue type.
     *
     * @return the queue type name
     */
    String queueType();

    /**
     * Gets the status of the queue after the players were added.
     *
     * @return the queue's status after the enqueue
     */
    QueueStatus queueStatus();

    /**
     * Gets the list of all player UUIDs currently in the queue (including the new ones).
     *
     * @return all player UUIDs currently in the queue
     */
    List<UUID> queuePlayerIds();

    /**
     * Gets the list of UUIDs of the players that were just added to the queue.
     *
     * @return the UUIDs of the players that were enqueued
     */
    List<UUID> playerIds();
}

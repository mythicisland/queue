package net.mythicisland.queue.api.event.player;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Fired when one or more players are added to a queue.
 */
public interface EnqueueEvent {

    /**
     * @return the unique ID of the queue the players joined
     */
    UUID queueId();

    /**
     * @return the queue type name
     */
    String queueType();

    /**
     * @return the queue's status after the enqueue
     */
    QueueStatus queueStatus();

    /**
     * @return all player UUIDs currently in the queue
     */
    List<UUID> queuePlayerIds();

    /**
     * @return the UUIDs of the players that were enqueued
     */
    List<UUID> playerIds();
}

package net.mythicisland.queue.api.event.player;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when one or more players are removed from a queue.
 *
 * @param queueId the unique ID of the queue the players left
 * @param queueType the queue type name
 * @param queueStatus the queue's status after the dequeue
 * @param queuePlayerIds all player UUIDs remaining in the queue
 * @param playerIds the UUIDs of the players that were dequeued
 */
public record DequeueEvent(
        UUID queueId,
        String queueType,
        QueueStatus queueStatus,
        List<UUID> queuePlayerIds,
        List<UUID> playerIds
) {
}

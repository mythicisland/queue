package net.mythicisland.queue.api.event.player;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when one or more players are added to a queue.
 *
 * @param queueId the unique ID of the queue the players joined
 * @param queueType the queue type name
 * @param queueStatus the queue's status after the enqueue
 * @param queuePlayerIds all player UUIDs currently in the queue (including the new ones)
 * @param playerIds the UUIDs of the players that were enqueued
 */
public record EnqueueEvent(
        UUID queueId,
        String queueType,
        QueueStatus queueStatus,
        List<UUID> queuePlayerIds,
        List<UUID> playerIds
) {
}

package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when a queue transitions from one lifecycle status to another.
 *
 * @param queueId the unique ID of the queue
 * @param queueType the queue type name
 * @param queuePlayerIds the player UUIDs in the queue
 * @param oldStatus the status before the transition
 * @param newStatus the status after the transition
 */
public record QueueStatusUpdatedEvent(
        UUID queueId,
        String queueType,
        List<UUID> queuePlayerIds,
        QueueStatus oldStatus,
        QueueStatus newStatus
) {
}

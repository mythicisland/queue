package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when a new queue instance is created.
 *
 * @param queueId the unique ID of the created queue
 * @param queueType the queue type name (e.g., "skyblock")
 * @param queueStatus the queue's initial status
 * @param queuePlayerIds the player UUIDs in the queue at creation time
 */
public record QueueCreatedEvent(
        UUID queueId,
        String queueType,
        QueueStatus queueStatus,
        List<UUID> queuePlayerIds
) {
}

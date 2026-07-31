package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when a queue instance is deleted, typically after it has finished its lifecycle.
 *
 * @param queueId the unique ID of the deleted queue
 * @param queueType the queue type name
 * @param queueStatus the queue's final status
 * @param queuePlayerIds the player UUIDs that were in the queue at deletion time
 */
public record QueueDeletedEvent(
        UUID queueId,
        String queueType,
        QueueStatus queueStatus,
        List<UUID> queuePlayerIds
) {
}

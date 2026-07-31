package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when any aspect of a queue changes (e.g., status or player list).
 *
 * <p>This event provides both the "before" and "after" snapshots, allowing consumers to
 * determine exactly what changed by comparing the two states.</p>
 *
 * @param queueId the unique ID of the queue
 * @param queueType the queue type name
 * @param beforeStatus the queue's status before the update
 * @param beforePlayerIds the player UUIDs before the update
 * @param afterStatus the queue's status after the update
 * @param afterPlayerIds the player UUIDs after the update
 */
public record QueueUpdatedEvent(
        UUID queueId,
        String queueType,
        QueueStatus beforeStatus,
        List<UUID> beforePlayerIds,
        QueueStatus afterStatus,
        List<UUID> afterPlayerIds
) {
}

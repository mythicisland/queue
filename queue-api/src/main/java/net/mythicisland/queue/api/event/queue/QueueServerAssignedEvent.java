package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when a game server is reserved and assigned to a specific queue.
 *
 * @param queueId the unique ID of the queue
 * @param queueType the queue type name
 * @param queueStatus the queue's current status
 * @param queuePlayerIds the player UUIDs in the queue
 * @param serverId the ID of the assigned server
 */
public record QueueServerAssignedEvent(
        UUID queueId,
        String queueType,
        QueueStatus queueStatus,
        List<UUID> queuePlayerIds,
        String serverId
) {
}

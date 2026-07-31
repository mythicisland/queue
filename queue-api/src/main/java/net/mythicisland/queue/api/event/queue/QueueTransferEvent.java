package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when players are being transferred to a game server.
 *
 * @param queueId the unique ID of the queue
 * @param queueType the queue type name
 * @param queueStatus the queue's current status
 * @param queuePlayerIds all player UUIDs in the queue
 * @param serverId the ID of the target server
 * @param transferredPlayerIds the UUIDs of the players that are being transferred
 */
public record QueueTransferEvent(
        UUID queueId,
        String queueType,
        QueueStatus queueStatus,
        List<UUID> queuePlayerIds,
        String serverId,
        List<UUID> transferredPlayerIds
) {
}

package net.mythicisland.queue.api.internal.event.queue;

import net.mythicisland.queue.api.event.queue.QueueTransferEvent;
import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

record QueueTransferEventImpl(
        UUID queueId,
        String queueType,
        QueueStatus queueStatus,
        List<UUID> queuePlayerIds,
        String serverId,
        List<UUID> transferredPlayerIds
) implements QueueTransferEvent {
}

package net.mythicisland.queue.api.internal.event.queue;

import net.mythicisland.queue.api.event.queue.QueueServerAssignedEvent;
import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

record QueueServerAssignedEventImpl(
        UUID queueId,
        String queueType,
        QueueStatus queueStatus,
        List<UUID> queuePlayerIds,
        String serverId
) implements QueueServerAssignedEvent {
}

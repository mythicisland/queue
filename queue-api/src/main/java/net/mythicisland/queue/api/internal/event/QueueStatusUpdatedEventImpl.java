package net.mythicisland.queue.api.internal.event;

import net.mythicisland.queue.api.event.queue.QueueStatusUpdatedEvent;
import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

record QueueStatusUpdatedEventImpl(
        UUID queueId,
        String queueType,
        List<UUID> queuePlayerIds,
        QueueStatus oldStatus,
        QueueStatus newStatus
) implements QueueStatusUpdatedEvent {
}

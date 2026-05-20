package net.mythicisland.queue.api.internal.event.queue;

import net.mythicisland.queue.api.event.queue.QueueUpdatedEvent;
import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

record QueueUpdatedEventImpl(
        UUID queueId,
        String queueType,
        QueueStatus beforeStatus,
        List<UUID> beforePlayerIds,
        QueueStatus afterStatus,
        List<UUID> afterPlayerIds
) implements QueueUpdatedEvent {
}

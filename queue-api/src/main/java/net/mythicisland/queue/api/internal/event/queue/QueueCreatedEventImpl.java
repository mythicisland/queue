package net.mythicisland.queue.api.internal.event.queue;

import net.mythicisland.queue.api.event.queue.QueueCreatedEvent;
import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

record QueueCreatedEventImpl(
        UUID queueId,
        String queueType,
        QueueStatus queueStatus,
        List<UUID> queuePlayerIds
) implements QueueCreatedEvent {
}

package net.mythicisland.queue.api.internal.event;

import net.mythicisland.queue.api.event.queue.QueueDeletedEvent;
import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

record QueueDeletedEventImpl(
        UUID queueId,
        String queueType,
        QueueStatus queueStatus,
        List<UUID> queuePlayerIds
) implements QueueDeletedEvent {
}

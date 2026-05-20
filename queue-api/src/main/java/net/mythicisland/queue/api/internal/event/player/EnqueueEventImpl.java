package net.mythicisland.queue.api.internal.event.player;

import net.mythicisland.queue.api.event.player.EnqueueEvent;
import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

record EnqueueEventImpl(
        UUID queueId,
        String queueType,
        QueueStatus queueStatus,
        List<UUID> queuePlayerIds,
        List<UUID> playerIds
) implements EnqueueEvent {
}

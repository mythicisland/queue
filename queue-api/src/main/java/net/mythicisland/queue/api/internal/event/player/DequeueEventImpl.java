package net.mythicisland.queue.api.internal.event.player;

import net.mythicisland.queue.api.event.player.DequeueEvent;
import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

record DequeueEventImpl(
        UUID queueId,
        String queueType,
        QueueStatus queueStatus,
        List<UUID> queuePlayerIds,
        List<UUID> playerIds
) implements DequeueEvent {
}

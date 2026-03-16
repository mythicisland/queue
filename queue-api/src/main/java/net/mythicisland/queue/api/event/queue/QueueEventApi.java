package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.event.Subscription;

import java.util.function.Consumer;

public interface QueueEventApi {

    Subscription onCreated(Consumer<QueueCreatedEvent> handler);

    Subscription onDeleted(Consumer<QueueDeletedEvent> handler);

    Subscription onServerAssigned(Consumer<QueueServerAssignedEvent> handler);

    Subscription onStatusUpdated(Consumer<QueueStatusUpdatedEvent> handler);

    Subscription onTransfer(Consumer<QueueTransferEvent> handler);

    Subscription onUpdated(Consumer<QueueUpdatedEvent> handler);

}

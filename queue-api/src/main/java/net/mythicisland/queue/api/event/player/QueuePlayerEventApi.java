package net.mythicisland.queue.api.event.player;

import net.mythicisland.queue.api.event.Subscription;

import java.util.function.Consumer;

public interface QueuePlayerEventApi {

    Subscription onEnqueued(Consumer<EnqueueEvent> handler);

    Subscription onDequeued(Consumer<DequeueEvent> handler);

}

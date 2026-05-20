package net.mythicisland.queue.api.event;

import net.mythicisland.queue.api.event.player.QueuePlayerEventApi;
import net.mythicisland.queue.api.event.queue.QueueEventApi;

/**
 * API for subscribing to events.
 */
public interface EventApi {

    /**
     * Provides access to queue lifecycle and status events.
     *
     * @return the queue event subscription API
     */
    QueueEventApi queue();

    /**
     * Provides access to events related to players.
     *
     * @return the player event subscription API
     */
    QueuePlayerEventApi player();

}

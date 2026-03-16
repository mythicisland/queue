package net.mythicisland.queue.api.event;

import net.mythicisland.queue.api.event.player.QueuePlayerEventApi;
import net.mythicisland.queue.api.event.queue.QueueEventApi;

public interface EventApi {

    QueueEventApi queue();

    QueuePlayerEventApi player();

}

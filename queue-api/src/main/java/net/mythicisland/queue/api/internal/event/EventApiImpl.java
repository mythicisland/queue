package net.mythicisland.queue.api.internal.event;

import io.nats.client.Connection;
import net.mythicisland.queue.api.event.EventApi;
import net.mythicisland.queue.api.event.player.QueuePlayerEventApi;
import net.mythicisland.queue.api.event.queue.QueueEventApi;
import net.mythicisland.queue.api.internal.event.player.QueuePlayerEventApiImpl;
import net.mythicisland.queue.api.internal.event.queue.QueueEventApiImpl;

public final class EventApiImpl implements EventApi {

    private final QueueEventApi queueEventApi;
    private final QueuePlayerEventApi playerEventApi;

    public EventApiImpl(Connection connection) {
        this.queueEventApi = new QueueEventApiImpl(connection);
        this.playerEventApi = new QueuePlayerEventApiImpl(connection);
    }

    @Override
    public QueueEventApi queue() {
        return queueEventApi;
    }

    @Override
    public QueuePlayerEventApi player() {
        return playerEventApi;
    }
}

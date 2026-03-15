package net.mythicisland.queue.api;

import net.mythicisland.queue.api.internal.QueueApiImpl;
import net.mythicisland.queue.api.player.QueuePlayerApi;

public interface QueueApi extends AutoCloseable {

    static QueueApi create() {
        return create(QueueApiOptions.DEFAULT);
    }

    static QueueApi create(QueueApiOptions options) {
        return new QueueApiImpl(options);
    }

    @Override
    void close();

    QueuePlayerApi player();
}

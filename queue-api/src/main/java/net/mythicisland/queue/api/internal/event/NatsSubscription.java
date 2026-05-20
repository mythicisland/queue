package net.mythicisland.queue.api.internal.event;

import io.nats.client.Dispatcher;
import net.mythicisland.queue.api.event.Subscription;

public final class NatsSubscription implements Subscription {

    private final Dispatcher dispatcher;
    private final String subject;

    public NatsSubscription(Dispatcher dispatcher, String subject) {
        this.dispatcher = dispatcher;
        this.subject = subject;
    }

    @Override
    public void unsubscribe() {
        dispatcher.unsubscribe(subject);
    }
}

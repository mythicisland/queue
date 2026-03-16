package net.mythicisland.queue.api.internal.event;

import net.mythicisland.queue.api.event.Subscription;

/**
 * A {@link Subscription} backed by a NATS dispatcher subscription.
 *
 * <p>Unsubscribing removes the subject from the dispatcher, stopping
 * message delivery for this particular handler.</p>
 */
final class NatsSubscription implements Subscription {

    private final io.nats.client.Dispatcher dispatcher;
    private final String subject;

    NatsSubscription(io.nats.client.Dispatcher dispatcher, String subject) {
        this.dispatcher = dispatcher;
        this.subject = subject;
    }

    @Override
    public void unsubscribe() {
        dispatcher.unsubscribe(subject);
    }
}

package net.mythicisland.queue.api.internal.event;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import net.mythicisland.queue.api.event.Subscription;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base for NATS backed event APIs, handling dispatcher setup and proto deserialization.
 */
public abstract class NatsEventApi {

    private static final Logger LOGGER = Logger.getLogger(NatsEventApi.class.getName());

    private final Connection connection;

    protected NatsEventApi(Connection connection) {
        this.connection = connection;
    }

    /**
     * Subscribes to a subject, deserializing each message and mapping it to an API event.
     *
     * @param subject the NATS subject to listen on
     * @param parser the parser for the proto message carried on the subject
     * @param mapper maps the parsed proto message to the API event
     * @param handler receives the mapped event
     * @param <P> the proto message type
     * @param <E> the API event type
     * @return a subscription handle to manage the listener lifecycle
     */
    protected <P, E> Subscription subscribe(
            String subject,
            Parser<P> parser,
            Function<P, E> mapper,
            Consumer<E> handler
    ) {
        Dispatcher dispatcher = connection.createDispatcher(message -> {
            try {
                handler.accept(mapper.apply(parser.parseFrom(message.getData())));
            } catch (InvalidProtocolBufferException e) {
                LOGGER.log(Level.WARNING, "Failed to deserialize message on subject " + subject, e);
            }
        });

        dispatcher.subscribe(subject);
        return new NatsSubscription(dispatcher, subject);
    }
}

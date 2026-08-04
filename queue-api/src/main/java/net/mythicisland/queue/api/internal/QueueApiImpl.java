package net.mythicisland.queue.api.internal;

import build.buf.gen.mythicisland.queue.v2.QueueDataServiceGrpc;
import build.buf.gen.mythicisland.queue.v2.TicketServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import net.mythicisland.moonrise.common.auth.AuthCredentials;
import net.mythicisland.queue.api.QueueApi;
import net.mythicisland.queue.api.QueueApiOptions;
import net.mythicisland.queue.api.data.QueueDataApi;
import net.mythicisland.queue.api.event.EventApi;
import net.mythicisland.queue.api.internal.data.QueueDataApiImpl;
import net.mythicisland.queue.api.internal.event.EventApiImpl;
import net.mythicisland.queue.api.internal.ticket.TicketApiImpl;
import net.mythicisland.queue.api.ticket.TicketApi;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class QueueApiImpl implements QueueApi {

    private final ManagedChannel channel;
    private final Connection nc;
    private final TicketApi ticketApi;
    private final QueueDataApi dataApi;
    private final EventApi eventApi;

    public QueueApiImpl(QueueApiOptions options) {
        this.channel = ManagedChannelBuilder
                .forAddress(options.grpcHost(), options.grpcPort())
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .build();

        try {
            this.nc = Nats.connect(
                    Options.builder()
                            .server(options.natsUrl())
                            .userInfo(options.natsUser(), options.natsSecret())
                            .maxReconnects(-1)
                            .build()
            );
        } catch (IOException | InterruptedException e) {
            channel.shutdownNow();
            throw new RuntimeException("Failed to establish NATS connection", e);
        }

        AuthCredentials credentials = new AuthCredentials(options.token());

        TicketServiceGrpc.TicketServiceFutureStub stub = TicketServiceGrpc.newFutureStub(channel)
                .withCallCredentials(credentials);
        this.ticketApi = new TicketApiImpl(stub);

        QueueDataServiceGrpc.QueueDataServiceFutureStub dataStub = QueueDataServiceGrpc.newFutureStub(channel)
                .withCallCredentials(credentials);
        this.dataApi = new QueueDataApiImpl(dataStub);

        this.eventApi = new EventApiImpl(nc);
    }

    @Override
    public void close() {
        try {
            nc.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            channel.shutdown();
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        } catch (InterruptedException e) {
            channel.shutdownNow();
        }
    }

    @Override
    public TicketApi ticket() {
        return ticketApi;
    }

    @Override
    public QueueDataApi data() {
        return dataApi;
    }

    @Override
    public EventApi event() {
        return eventApi;
    }
}

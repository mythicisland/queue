package net.mythicisland.queue.api.internal;

import build.buf.gen.mythicisland.queue.v1.QueueDataServiceGrpc;
import build.buf.gen.mythicisland.queue.v1.QueueServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.mythicisland.queue.api.QueueApi;
import net.mythicisland.queue.api.QueueApiOptions;
import net.mythicisland.queue.api.data.QueueDataApi;
import net.mythicisland.queue.api.internal.data.QueueDataApiImpl;
import net.mythicisland.queue.api.internal.nats.NatsFailoverConnectionManager;
import net.mythicisland.queue.api.internal.player.QueuePlayerApiImpl;
import net.mythicisland.queue.api.player.QueuePlayerApi;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class QueueApiImpl implements QueueApi {

    private static final Logger LOGGER = Logger.getLogger(QueueApiImpl.class.getName());

    private final ManagedChannel grpcChannel;
    private final NatsFailoverConnectionManager natsManager;
    private final QueuePlayerApi playerApi;
    private final QueueDataApi dataApi;

    public QueueApiImpl(QueueApiOptions options) {
        this.grpcChannel = ManagedChannelBuilder
                .forAddress(options.getGrpcHost(), options.getGrpcPort())
                .usePlaintext()
                .build();

        NatsFailoverConnectionManager nats;
        try {
            nats = new NatsFailoverConnectionManager(
                    options.getNatsUrl(),
                    options.getNatsUser(),
                    options.getNatsSecret(),
                    options.getNatsFailoverReconnectAfter()
            );
        } catch (IOException | InterruptedException e) {
            grpcChannel.shutdownNow();
            throw new RuntimeException("Failed to establish NATS connection", e);
        }
        this.natsManager = nats;

        QueueServiceGrpc.QueueServiceFutureStub stub = QueueServiceGrpc.newFutureStub(grpcChannel);
        this.playerApi = new QueuePlayerApiImpl(stub);

        QueueDataServiceGrpc.QueueDataServiceFutureStub dataStub = QueueDataServiceGrpc.newFutureStub(grpcChannel);
        this.dataApi = new QueueDataApiImpl(dataStub);
    }

    @Override
    public void close() {
        try {
            natsManager.shutdown();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to shutdown NATS connection manager", e);
        }

        try {
            grpcChannel.shutdown();
            if (!grpcChannel.awaitTermination(5, TimeUnit.SECONDS)) {
                grpcChannel.shutdownNow();
            }
        } catch (InterruptedException e) {
            grpcChannel.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public QueuePlayerApi player() {
        return playerApi;
    }

    @Override
    public QueueDataApi data() {
        return dataApi;
    }
}

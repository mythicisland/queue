package net.mythicisland.queue.api.internal.player;

import build.buf.gen.mythicisland.queue.v1.*;
import net.mythicisland.queue.api.player.QueuePlayerApi;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.mythicisland.queue.api.internal.ProtoUtil.toCompletableFuture;

public class QueuePlayerApiImpl implements QueuePlayerApi {

    private final QueueServiceGrpc.QueueServiceFutureStub stub;

    public QueuePlayerApiImpl(QueueServiceGrpc.QueueServiceFutureStub stub) {
        this.stub = stub;
    }

    @Override
    public CompletableFuture<EnqueueResponse> enqueue(String type, List<UUID> playerIds) {
        List<String> ids = playerIds.stream().map(UUID::toString).toList();

        return toCompletableFuture(stub.enqueue(
                EnqueueRequest.newBuilder()
                        .setType(type)
                        .addAllPlayerIds(ids)
                        .build()
        ));
    }

    @Override
    public CompletableFuture<DequeueResponse> dequeue(List<UUID> playerIds) {
        List<String> ids = playerIds.stream().map(UUID::toString).toList();

        return toCompletableFuture(stub.dequeue(
                DequeueRequest.newBuilder()
                        .addAllPlayerIds(ids)
                        .build()
        ));
    }

}

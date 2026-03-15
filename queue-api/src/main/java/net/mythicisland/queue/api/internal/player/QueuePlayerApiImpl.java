package net.mythicisland.queue.api.internal.player;

import build.buf.gen.mythicisland.queue.v1.*;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.mythicisland.queue.api.player.QueuePlayerApi;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class QueuePlayerApiImpl implements QueuePlayerApi {

    private final QueueServiceGrpc.QueueServiceFutureStub stub;
    private final Executor executor;

    public QueuePlayerApiImpl(QueueServiceGrpc.QueueServiceFutureStub stub) {
        this.stub = stub;
        this.executor = ForkJoinPool.commonPool();
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

    private <T> CompletableFuture<T> toCompletableFuture(ListenableFuture<T> listenableFuture) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Futures.addCallback(listenableFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(T result) {
                future.complete(result);
            }

            @Override
            public void onFailure(@NotNull Throwable t) {
                future.completeExceptionally(t);
            }
        }, executor);
        return future;
    }
}

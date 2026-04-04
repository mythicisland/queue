package net.mythicisland.queue.api.internal.data;

import build.buf.gen.mythicisland.queue.v1.*;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.mythicisland.queue.api.data.QueueDataApi;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class QueueDataApiImpl implements QueueDataApi {

    private final QueueDataServiceGrpc.QueueDataServiceFutureStub stub;
    private final Executor executor;

    public QueueDataApiImpl(QueueDataServiceGrpc.QueueDataServiceFutureStub stub) {
        this.stub = stub;
        this.executor = ForkJoinPool.commonPool();
    }

    @Override
    public CompletableFuture<GetQueueResponse> getQueue(UUID queueId) {
        return toCompletableFuture(stub.getQueue(
                GetQueueRequest.newBuilder()
                        .setQueueId(queueId.toString())
                        .build()
        ));
    }

    @Override
    public CompletableFuture<GetAllQueuesResponse> getAllQueues() {
        return toCompletableFuture(stub.getAllQueues(
                GetAllQueuesRequest.getDefaultInstance()
        ));
    }

    @Override
    public CompletableFuture<GetQueuesByTypeResponse> getQueuesByType(String type) {
        return toCompletableFuture(stub.getQueuesByType(
                GetQueuesByTypeRequest.newBuilder()
                        .setType(type)
                        .build()
        ));
    }

    @Override
    public CompletableFuture<GetQueueByPlayerResponse> getQueueByPlayer(UUID playerId) {
        return toCompletableFuture(stub.getQueueByPlayer(
                GetQueueByPlayerRequest.newBuilder()
                        .setPlayerId(playerId.toString())
                        .build()
        ));
    }

    @Override
    public CompletableFuture<GetPlayerPositionResponse> getPlayerPosition(UUID playerId) {
        return toCompletableFuture(stub.getPlayerPosition(
                GetPlayerPositionRequest.newBuilder()
                        .setPlayerId(playerId.toString())
                        .build()
        ));
    }

    @Override
    public CompletableFuture<GetQueueTypeResponse> getQueueType(String name) {
        return toCompletableFuture(stub.getQueueType(
                GetQueueTypeRequest.newBuilder()
                        .setName(name)
                        .build()
        ));
    }

    @Override
    public CompletableFuture<GetAllQueueTypesResponse> getAllQueueTypes() {
        return toCompletableFuture(stub.getAllQueueTypes(
                GetAllQueueTypesRequest.getDefaultInstance()
        ));
    }

    @Override
    public CompletableFuture<GetQueueTypeStatsResponse> getQueueTypeStats(String name) {
        return toCompletableFuture(stub.getQueueTypeStats(
                GetQueueTypeStatsRequest.newBuilder()
                        .setName(name)
                        .build()
        ));
    }

    @Override
    public CompletableFuture<GetAllQueueTypeStatsResponse> getAllQueueTypeStats() {
        return toCompletableFuture(stub.getAllQueueTypeStats(
                GetAllQueueTypeStatsRequest.getDefaultInstance()
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

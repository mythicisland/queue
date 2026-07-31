package net.mythicisland.queue.api.internal.data;

import build.buf.gen.mythicisland.queue.v1.*;
import net.mythicisland.queue.api.data.QueueDataApi;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.mythicisland.queue.api.internal.ProtoUtil.toCompletableFuture;

public class QueueDataApiImpl implements QueueDataApi {

    private final QueueDataServiceGrpc.QueueDataServiceFutureStub stub;

    public QueueDataApiImpl(QueueDataServiceGrpc.QueueDataServiceFutureStub stub) {
        this.stub = stub;
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

}

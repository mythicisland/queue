package net.mythicisland.queue.api.internal.data;

import build.buf.gen.mythicisland.queue.v2.*;
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
    public CompletableFuture<GetTicketResponse> getTicket(UUID ticketId) {
        return toCompletableFuture(stub.getTicket(
                GetTicketRequest.newBuilder()
                        .setTicketId(ticketId.toString())
                        .build()
        ));
    }

    @Override
    public CompletableFuture<GetTicketByPlayerResponse> getTicketByPlayer(UUID playerId) {
        return toCompletableFuture(stub.getTicketByPlayer(
                GetTicketByPlayerRequest.newBuilder()
                        .setPlayerId(playerId.toString())
                        .build()
        ));
    }

    @Override
    public CompletableFuture<ListTicketsResponse> listTickets() {
        return toCompletableFuture(stub.listTickets(
                ListTicketsRequest.getDefaultInstance()
        ));
    }

    @Override
    public CompletableFuture<ListTicketsResponse> listTickets(String queueType) {
        return toCompletableFuture(stub.listTickets(
                ListTicketsRequest.newBuilder()
                        .setQueueType(queueType)
                        .build()
        ));
    }

    @Override
    public CompletableFuture<GetMatchResponse> getMatch(UUID matchId) {
        return toCompletableFuture(stub.getMatch(
                GetMatchRequest.newBuilder()
                        .setMatchId(matchId.toString())
                        .build()
        ));
    }

    @Override
    public CompletableFuture<ListMatchesResponse> listMatches() {
        return toCompletableFuture(stub.listMatches(
                ListMatchesRequest.getDefaultInstance()
        ));
    }

    @Override
    public CompletableFuture<ListMatchesResponse> listMatches(String queueType) {
        return toCompletableFuture(stub.listMatches(
                ListMatchesRequest.newBuilder()
                        .setQueueType(queueType)
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
    public CompletableFuture<ListQueueTypesResponse> listQueueTypes() {
        return toCompletableFuture(stub.listQueueTypes(
                ListQueueTypesRequest.getDefaultInstance()
        ));
    }

    @Override
    public CompletableFuture<GetQueueStatsResponse> getQueueStats(String queueType) {
        return toCompletableFuture(stub.getQueueStats(
                GetQueueStatsRequest.newBuilder()
                        .setQueueType(queueType)
                        .build()
        ));
    }

    @Override
    public CompletableFuture<ListQueueStatsResponse> listQueueStats() {
        return toCompletableFuture(stub.listQueueStats(
                ListQueueStatsRequest.getDefaultInstance()
        ));
    }

}

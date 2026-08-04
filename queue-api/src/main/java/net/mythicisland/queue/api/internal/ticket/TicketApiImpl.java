package net.mythicisland.queue.api.internal.ticket;

import build.buf.gen.mythicisland.queue.v2.*;
import net.mythicisland.queue.api.ticket.TicketApi;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.mythicisland.queue.api.internal.ProtoUtil.toCompletableFuture;

public class TicketApiImpl implements TicketApi {

    private final TicketServiceGrpc.TicketServiceFutureStub stub;

    public TicketApiImpl(TicketServiceGrpc.TicketServiceFutureStub stub) {
        this.stub = stub;
    }

    @Override
    public CompletableFuture<CreateTicketResponse> createTicket(List<UUID> playerIds, List<String> queueTypes) {
        List<String> ids = playerIds.stream().map(UUID::toString).toList();

        return toCompletableFuture(stub.createTicket(
                CreateTicketRequest.newBuilder()
                        .addAllPlayerIds(ids)
                        .addAllQueueTypes(queueTypes)
                        .build()
        ));
    }

    @Override
    public CompletableFuture<DeleteTicketResponse> deleteTicket(UUID ticketId) {
        return toCompletableFuture(stub.deleteTicket(
                DeleteTicketRequest.newBuilder()
                        .setTicketId(ticketId.toString())
                        .build()
        ));
    }

    @Override
    public CompletableFuture<DeleteTicketResponse> deleteTicketByPlayer(UUID playerId) {
        return toCompletableFuture(stub.deleteTicket(
                DeleteTicketRequest.newBuilder()
                        .setPlayerId(playerId.toString())
                        .build()
        ));
    }

}

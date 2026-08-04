package net.mythicisland.queue.api.internal;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Timestamp;
import net.mythicisland.queue.api.match.Assignment;
import net.mythicisland.queue.api.match.Match;
import net.mythicisland.queue.api.match.MatchState;
import net.mythicisland.queue.api.ticket.Ticket;
import net.mythicisland.queue.api.ticket.TicketDeleteReason;
import net.mythicisland.queue.api.ticket.TicketState;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public final class ProtoUtil {

    private ProtoUtil() {}

    private static final Executor executor = ForkJoinPool.commonPool();

    public static <T> CompletableFuture<T> toCompletableFuture(ListenableFuture<T> listenableFuture) {
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

    public static List<UUID> toUuidList(List<String> ids) {
        return ids.stream().map(UUID::fromString).toList();
    }

    public static Ticket toTicket(build.buf.gen.mythicisland.queue.v2.Ticket proto) {
        return new Ticket(
                UUID.fromString(proto.getId()),
                toUuidList(proto.getPlayerIdsList()),
                List.copyOf(proto.getQueueTypesList()),
                toTicketState(proto.getState()),
                toInstant(proto.getCreatedAt()),
                proto.getMatchId().isEmpty() ? null : UUID.fromString(proto.getMatchId()),
                proto.hasAssignment() ? toAssignment(proto.getAssignment()) : null,
                proto.hasCountdownEndTime() ? toInstant(proto.getCountdownEndTime()) : null
        );
    }

    public static Match toMatch(build.buf.gen.mythicisland.queue.v2.Match proto) {
        return new Match(
                UUID.fromString(proto.getId()),
                proto.getQueueType(),
                proto.getTicketsList().stream().map(ProtoUtil::toTicket).toList(),
                toMatchState(proto.getState()),
                toInstant(proto.getCreatedAt()),
                proto.hasAssignment() ? toAssignment(proto.getAssignment()) : null,
                proto.hasCountdownEndTime() ? toInstant(proto.getCountdownEndTime()) : null
        );
    }

    public static Assignment toAssignment(build.buf.gen.mythicisland.queue.v2.Assignment proto) {
        return new Assignment(proto.getServerId(), proto.getServerName());
    }

    public static Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    public static TicketState toTicketState(build.buf.gen.mythicisland.queue.v2.TicketState state) {
        return switch (state) {
            case TICKET_STATE_SEARCHING -> TicketState.SEARCHING;
            case TICKET_STATE_MATCHED -> TicketState.MATCHED;
            case TICKET_STATE_ASSIGNED -> TicketState.ASSIGNED;
            default -> throw new IllegalArgumentException("Unknown TicketState: " + state);
        };
    }

    public static MatchState toMatchState(build.buf.gen.mythicisland.queue.v2.MatchState state) {
        return switch (state) {
            case MATCH_STATE_ALLOCATING -> MatchState.ALLOCATING;
            case MATCH_STATE_COUNTDOWN -> MatchState.COUNTDOWN;
            case MATCH_STATE_TRANSFERRING -> MatchState.TRANSFERRING;
            case MATCH_STATE_COMPLETED -> MatchState.COMPLETED;
            case MATCH_STATE_FAILED -> MatchState.FAILED;
            default -> throw new IllegalArgumentException("Unknown MatchState: " + state);
        };
    }

    public static TicketDeleteReason toDeleteReason(build.buf.gen.mythicisland.queue.v2.TicketDeleteReason reason) {
        return switch (reason) {
            case TICKET_DELETE_REASON_CANCELLED -> TicketDeleteReason.CANCELLED;
            case TICKET_DELETE_REASON_TRANSFERRED -> TicketDeleteReason.TRANSFERRED;
            case TICKET_DELETE_REASON_EXPIRED -> TicketDeleteReason.EXPIRED;
            default -> throw new IllegalArgumentException("Unknown TicketDeleteReason: " + reason);
        };
    }
}

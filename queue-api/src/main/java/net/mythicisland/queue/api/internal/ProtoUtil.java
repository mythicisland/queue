package net.mythicisland.queue.api.internal;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.mythicisland.queue.api.queue.QueueStatus;
import org.jetbrains.annotations.NotNull;

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

    public static QueueStatus toApiStatus(build.buf.gen.mythicisland.queue.v1.QueueStatus status) {
        return switch (status) {
            case NOT_ENOUGH_PLAYERS -> QueueStatus.NOT_ENOUGH_PLAYERS;
            case WAITING_COUNTDOWN -> QueueStatus.WAITING_COUNTDOWN;
            case SEARCHING_SERVER -> QueueStatus.SEARCHING_SERVER;
            case WAITING_FOR_SERVER -> QueueStatus.WAITING_FOR_SERVER;
            case SERVER_READY -> QueueStatus.SERVER_READY;
            case COUNTDOWN -> QueueStatus.COUNTDOWN;
            case TELEPORTING -> QueueStatus.TELEPORTING;
            case FINISHED -> QueueStatus.FINISHED;
            default -> throw new IllegalArgumentException("Unknown QueueStatus: " + status);
        };
    }

    public static List<UUID> toUuidList(List<String> ids) {
        return ids.stream().map(UUID::fromString).toList();
    }

    public static UUID toQueueId(build.buf.gen.mythicisland.queue.v1.Queue proto) {
        return UUID.fromString(proto.getUniqueId());
    }

    /**
     * Unpacks the fields shared by every queue event and hands them to {@code factory}.
     */
    public static <E> E fromQueue(build.buf.gen.mythicisland.queue.v1.Queue queue, QueueFactory<E> factory) {
        return factory.create(
                toQueueId(queue),
                queue.getType(),
                toApiStatus(queue.getStatus()),
                toUuidList(queue.getPlayerIdsList())
        );
    }

    /**
     * Builds an event from the fields shared by every queue event.
     *
     * @param <E> the API event type
     */
    @FunctionalInterface
    public interface QueueFactory<E> {
        E create(UUID queueId, String queueType, QueueStatus queueStatus, List<UUID> queuePlayerIds);
    }
}

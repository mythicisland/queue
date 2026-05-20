package net.mythicisland.queue.api.internal;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

public final class ProtoUtil {

    private ProtoUtil() {}

    public static QueueStatus toApiStatus(build.buf.gen.mythicisland.queue.v1.QueueStatus proto) {
        return switch (proto) {
            case NOT_ENOUGH_PLAYERS -> QueueStatus.NOT_ENOUGH_PLAYERS;
            case WAITING_COUNTDOWN -> QueueStatus.WAITING_COUNTDOWN;
            case SEARCHING_SERVER -> QueueStatus.SEARCHING_SERVER;
            case WAITING_FOR_SERVER -> QueueStatus.WAITING_FOR_SERVER;
            case SERVER_READY -> QueueStatus.SERVER_READY;
            case COUNTDOWN -> QueueStatus.COUNTDOWN;
            case TELEPORTING -> QueueStatus.TELEPORTING;
            case FINISHED -> QueueStatus.FINISHED;
            default -> throw new IllegalArgumentException("Unknown proto QueueStatus: " + proto);
        };
    }

    public static List<UUID> toUuidList(List<String> ids) {
        return ids.stream().map(UUID::fromString).toList();
    }

    public static UUID toQueueId(build.buf.gen.mythicisland.queue.v1.Queue proto) {
        return UUID.fromString(proto.getUniqueId());
    }
}

package net.mythicisland.queue.api.internal;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Converts between protobuf-generated types and the public API types.
 *
 * <p>This utility shields API consumers from any protobuf dependency,
 * translating proto enums, UUID strings, and queue messages into their
 * public-facing equivalents.</p>
 */
public final class ProtoConversionUtil {

    /**
     * Converts a protobuf {@link build.buf.gen.mythicisland.queue.v1.QueueStatus}
     * to the public API {@link QueueStatus}.
     *
     * @param proto the protobuf queue status
     * @return the corresponding API queue status
     */
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

    /**
     * @param ids the UUID strings from protobuf messages
     * @return the parsed UUIDs
     */
    public static List<UUID> toUuidList(List<String> ids) {
        return ids.stream().map(UUID::fromString).toList();
    }

    /**
     * Converts a protobuf {@link build.buf.gen.mythicisland.queue.v1.Queue}
     * to its unique ID as a {@link UUID}.
     *
     * @param proto the protobuf queue message
     * @return the queue's unique ID
     */
    public static UUID toQueueId(build.buf.gen.mythicisland.queue.v1.Queue proto) {
        return UUID.fromString(proto.getUniqueId());
    }

    /**
     * Hidden constructor.
     */
    private ProtoConversionUtil() {}
}

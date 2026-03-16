package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Fired when any aspect of a queue changes (status, players, etc.).
 *
 * <p>Provides both the before and after snapshots so consumers can
 * determine exactly what changed.</p>
 */
public interface QueueUpdatedEvent {

    /**
     * @return the unique ID of the queue
     */
    UUID queueId();

    /**
     * @return the queue type name
     */
    String queueType();

    /**
     * @return the queue's status before the update
     */
    QueueStatus beforeStatus();

    /**
     * @return the player UUIDs before the update
     */
    List<UUID> beforePlayerIds();

    /**
     * @return the queue's status after the update
     */
    QueueStatus afterStatus();

    /**
     * @return the player UUIDs after the update
     */
    List<UUID> afterPlayerIds();
}

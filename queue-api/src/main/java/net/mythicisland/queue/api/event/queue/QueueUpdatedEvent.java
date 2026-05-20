package net.mythicisland.queue.api.event.queue;

import net.mythicisland.queue.api.queue.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when any aspect of a queue changes (e.g., status or player list).
 *
 * <p>This event provides both the "before" and "after" snapshots, allowing consumers to
 * determine exactly what changed by comparing the two states.</p>
 */
public interface QueueUpdatedEvent {

    /**
     * Gets the unique identifier of the queue.
     *
     * @return the unique ID of the queue
     */
    UUID queueId();

    /**
     * Gets the name of the queue type.
     *
     * @return the queue type name
     */
    String queueType();

    /**
     * Gets the status of the queue before this update.
     *
     * @return the queue's status before the update
     */
    QueueStatus beforeStatus();

    /**
     * Gets the list of player UUIDs that were in the queue before this update.
     *
     * @return the player UUIDs before the update
     */
    List<UUID> beforePlayerIds();

    /**
     * Gets the status of the queue after this update.
     *
     * @return the queue's status after the update
     */
    QueueStatus afterStatus();

    /**
     * Gets the list of player UUIDs in the queue after this update.
     *
     * @return the player UUIDs after the update
     */
    List<UUID> afterPlayerIds();
}

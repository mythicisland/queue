package net.mythicisland.queue.api.type

import net.mythicisland.queue.api.QueueException
import java.util.concurrent.CompletableFuture

/**
 * Creating, updating and deleting queue types. Reading them is part of [net.mythicisland.queue.api.data.QueueDataApi].
 *
 * When the queue loads its queue types from files, the files are the only source and every call that changes
 * something throws a [QueueException] with [QueueException.Code.FAILED_PRECONDITION].
 */
interface QueueTypeApi {

    /**
     * Creates [type] and returns it. With [validateOnly] the queue only checks [type], also when it loads its
     * queue types from files.
     *
     * Throws a [QueueException] with [QueueException.Code.INVALID_ARGUMENT] if the name or the config is invalid,
     * the message lists all problems, and with [QueueException.Code.ALREADY_EXISTS] if the name is taken.
     */
    suspend fun createQueueType(type: QueueType, validateOnly: Boolean = false): QueueType

    /**
     * Replaces the whole config of the queue type named like [type] and returns it. With [validateOnly] the queue
     * only checks [type], also when it loads its queue types from files.
     *
     * Running matches keep going, tickets that don't fit the new config anymore expire.
     *
     * Throws a [QueueException] with [QueueException.Code.INVALID_ARGUMENT] if the config is invalid,
     * the message lists all problems, and with [QueueException.Code.NOT_FOUND] if there is no such queue type.
     */
    suspend fun updateQueueType(type: QueueType, validateOnly: Boolean = false): QueueType

    /**
     * Deletes the queue type with the given [name]. Tickets without any queue type left are deleted,
     * running matches keep going.
     *
     * Throws a [QueueException] with [QueueException.Code.NOT_FOUND] if there is no such queue type.
     */
    suspend fun deleteQueueType(name: String)

    /**
     * Activates or deactivates the queue type with the given [name] and returns it. Searching tickets without any
     * active queue type left are deleted, running matches keep going.
     *
     * Throws a [QueueException] with [QueueException.Code.NOT_FOUND] if there is no such queue type.
     */
    suspend fun setQueueTypeActive(name: String, active: Boolean): QueueType

    /**
     * Same as [createQueueType], but returns a [CompletableFuture].
     */
    fun createQueueTypeAsync(type: QueueType, validateOnly: Boolean): CompletableFuture<QueueType>

    /**
     * Same as [createQueueType], but returns a [CompletableFuture].
     */
    fun createQueueTypeAsync(type: QueueType): CompletableFuture<QueueType> = createQueueTypeAsync(type, false)

    /**
     * Same as [updateQueueType], but returns a [CompletableFuture].
     */
    fun updateQueueTypeAsync(type: QueueType, validateOnly: Boolean): CompletableFuture<QueueType>

    /**
     * Same as [updateQueueType], but returns a [CompletableFuture].
     */
    fun updateQueueTypeAsync(type: QueueType): CompletableFuture<QueueType> = updateQueueTypeAsync(type, false)

    /**
     * Same as [deleteQueueType], but returns a [CompletableFuture].
     */
    fun deleteQueueTypeAsync(name: String): CompletableFuture<Void?>

    /**
     * Same as [setQueueTypeActive], but returns a [CompletableFuture].
     */
    fun setQueueTypeActiveAsync(name: String, active: Boolean): CompletableFuture<QueueType>

}

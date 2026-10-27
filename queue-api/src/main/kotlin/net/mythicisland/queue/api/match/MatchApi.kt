package net.mythicisland.queue.api.match

import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Accepting, rejecting and releasing matches, used by game servers.
 */
interface MatchApi {

    /**
     * Accepts a match offered to [serverId] and returns it, the countdown starts right away.
     *
     * Fails with `FAILED_PRECONDITION` if the offer already timed out.
     */
    suspend fun acceptMatch(serverId: String, matchId: UUID): Match

    /**
     * Rejects a match offered to [serverId]. It won't be offered to this server again.
     */
    suspend fun rejectMatch(serverId: String, matchId: UUID, reason: String)

    /**
     * Releases a finished match, which frees its place on [serverId].
     */
    suspend fun releaseMatch(serverId: String, matchId: UUID)

    /**
     * Same as [acceptMatch], but returns a [CompletableFuture].
     */
    fun acceptMatchAsync(serverId: String, matchId: UUID): CompletableFuture<Match>

    /**
     * Same as [rejectMatch], but returns a [CompletableFuture].
     */
    fun rejectMatchAsync(serverId: String, matchId: UUID, reason: String): CompletableFuture<Void?>

    /**
     * Same as [releaseMatch], but returns a [CompletableFuture].
     */
    fun releaseMatchAsync(serverId: String, matchId: UUID): CompletableFuture<Void?>

}

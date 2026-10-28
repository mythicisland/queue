package net.mythicisland.queue.api.match

import net.mythicisland.queue.api.QueueApiOptions
import net.mythicisland.queue.api.QueueException
import net.mythicisland.queue.api.event.Subscription
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.function.Function

/**
 * Accepting, rejecting and releasing matches, used by game servers.
 */
interface MatchApi {

    /**
     * Accepts a match offered to [serverId] and returns it, the countdown starts right away.
     *
     * Throws a [QueueException] with [QueueException.Code.FAILED_PRECONDITION] if the offer already timed out.
     */
    suspend fun acceptMatch(serverId: String, matchId: UUID): Match

    /**
     * Same as [acceptMatch], for the server set in [QueueApiOptions.serverId].
     *
     * @throws IllegalStateException if [QueueApiOptions.serverId] is not set.
     */
    suspend fun acceptMatch(matchId: UUID): Match

    /**
     * Rejects a match offered to [serverId]. It won't be offered to this server again.
     */
    suspend fun rejectMatch(serverId: String, matchId: UUID, reason: String)

    /**
     * Same as [rejectMatch], for the server set in [QueueApiOptions.serverId].
     *
     * @throws IllegalStateException if [QueueApiOptions.serverId] is not set.
     */
    suspend fun rejectMatch(matchId: UUID, reason: String)

    /**
     * Releases a finished match, which frees its place on [serverId].
     */
    suspend fun releaseMatch(serverId: String, matchId: UUID)

    /**
     * Same as [releaseMatch], for the server set in [QueueApiOptions.serverId].
     *
     * @throws IllegalStateException if [QueueApiOptions.serverId] is not set.
     */
    suspend fun releaseMatch(matchId: UUID)

    /**
     * Same as [acceptMatch], but returns a [CompletableFuture].
     */
    fun acceptMatchAsync(serverId: String, matchId: UUID): CompletableFuture<Match>

    /**
     * Same as [acceptMatch], but returns a [CompletableFuture].
     *
     * @throws IllegalStateException if [QueueApiOptions.serverId] is not set.
     */
    fun acceptMatchAsync(matchId: UUID): CompletableFuture<Match>

    /**
     * Same as [rejectMatch], but returns a [CompletableFuture].
     */
    fun rejectMatchAsync(serverId: String, matchId: UUID, reason: String): CompletableFuture<Void?>

    /**
     * Same as [rejectMatch], but returns a [CompletableFuture].
     *
     * @throws IllegalStateException if [QueueApiOptions.serverId] is not set.
     */
    fun rejectMatchAsync(matchId: UUID, reason: String): CompletableFuture<Void?>

    /**
     * Same as [releaseMatch], but returns a [CompletableFuture].
     */
    fun releaseMatchAsync(serverId: String, matchId: UUID): CompletableFuture<Void?>

    /**
     * Same as [releaseMatch], but returns a [CompletableFuture].
     *
     * @throws IllegalStateException if [QueueApiOptions.serverId] is not set.
     */
    fun releaseMatchAsync(matchId: UUID): CompletableFuture<Void?>

    /**
     * Calls [handler] for every match offered to [serverId] and accepts or rejects it with the returned [OfferDecision].
     *
     * The match is rejected if [handler] throws. If the decision can't be sent, e.g. because the offer already
     * timed out, the offer is withdrawn and shows up as a state change in [EventApi.match][net.mythicisland.queue.api.event.EventApi.match].
     */
    fun onOffered(serverId: String, handler: Function<Match, OfferDecision>): Subscription

    /**
     * Same as [onOffered], for the server set in [QueueApiOptions.serverId].
     *
     * @throws IllegalStateException if [QueueApiOptions.serverId] is not set.
     */
    fun onOffered(handler: Function<Match, OfferDecision>): Subscription

}

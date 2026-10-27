package net.mythicisland.queue.api.match

import java.util.UUID

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

}

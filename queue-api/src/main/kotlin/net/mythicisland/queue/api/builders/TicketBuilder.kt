package net.mythicisland.queue.api.builders

import net.mythicisland.queue.api.QueueDsl
import java.util.UUID

/**
 * Describes the ticket that should enter matchmaking.
 *
 * A ticket holds either a single player or a whole party, and searches in one
 * or more queue types. Several queue types mean the party joins whichever match
 * fills up first.
 *
 * ```
 * api.ticket().create {
 *     party(leader, member)
 *     queues("battle", "skywars")
 * }
 * ```
 */
@QueueDsl
class TicketBuilder internal constructor() {

    private val playerIds = mutableListOf<UUID>()
    private val queueTypes = mutableListOf<String>()

    /**
     * Adds a single player to the ticket.
     *
     * @param playerId the UUID of the player.
     */
    fun player(playerId: UUID) {
        playerIds.add(playerId)
    }

    /**
     * Adds a group of players that must stay together.
     *
     * @param playerIds the UUIDs of the party members.
     */
    fun party(vararg playerIds: UUID) {
        party(playerIds.asList())
    }

    /**
     * Adds a group of players that must stay together.
     *
     * @param playerIds the UUIDs of the party members.
     */
    fun party(playerIds: Collection<UUID>) {
        this.playerIds.addAll(playerIds)
    }

    /**
     * Adds queue types to search in.
     *
     * @param queueTypes the names of the queue types.
     */
    fun queues(vararg queueTypes: String) {
        queues(queueTypes.asList())
    }

    /**
     * Adds queue types to search in.
     *
     * @param queueTypes the names of the queue types.
     */
    fun queues(queueTypes: Collection<String>) {
        this.queueTypes.addAll(queueTypes)
    }

    /**
     * Fails early instead of letting the server reject an empty request.
     */
    internal fun validate() {
        require(playerIds.isNotEmpty()) { "A ticket needs at least one player" }
        require(queueTypes.isNotEmpty()) { "A ticket needs at least one queue type" }
    }

    internal fun playerIds(): List<UUID> = playerIds.toList()

    internal fun queueTypes(): List<String> = queueTypes.toList()

}

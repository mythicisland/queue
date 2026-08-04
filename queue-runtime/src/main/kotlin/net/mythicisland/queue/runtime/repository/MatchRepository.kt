package net.mythicisland.queue.runtime.repository

import build.buf.gen.mythicisland.queue.v2.MatchState
import net.mythicisland.queue.shared.match.Match
import org.apache.logging.log4j.LogManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds every match that has not finished yet.
 *
 * Matches only live for a few seconds, from being formed until their players
 * are on the game server.
 */
class MatchRepository {

    private val logger = LogManager.getLogger(MatchRepository::class.java)

    private val lock = Any()
    private val matches = ConcurrentHashMap<UUID, Match>()
    private val ticketToMatch = ConcurrentHashMap<UUID, UUID>()

    /**
     * Gets a match by its id.
     */
    fun get(id: UUID): Match? {
        return matches[id]
    }

    /**
     * Gets the match a ticket was put into.
     */
    fun getByTicket(ticketId: UUID): Match? {
        return ticketToMatch[ticketId]?.let { matches[it] }
    }

    /**
     * Gets every active match.
     */
    fun getAll(): List<Match> {
        return matches.values.toList()
    }

    /**
     * Gets every active match of a queue type.
     */
    fun getAllByType(queueType: String): List<Match> {
        return matches.values.filter { it.queueType == queueType }
    }

    /**
     * Gets every active match in a state.
     */
    fun getAllByState(state: MatchState): List<Match> {
        return matches.values.filter { it.state == state }
    }

    /**
     * Adds a newly formed match.
     */
    fun add(match: Match) {
        synchronized(lock) {
            matches[match.id] = match
            match.ticketIds.forEach { ticketToMatch[it] = match.id }
        }
    }

    /**
     * Replaces a match with an updated copy.
     *
     * @return the stored match, or null if it was removed in the meantime.
     */
    fun update(match: Match): Match? {
        synchronized(lock) {
            if (!matches.containsKey(match.id)) {
                logger.debug("Skipped update of match {}, it is no longer stored", match.id)
                return null
            }

            matches[match.id] = match
            return match
        }
    }

    /**
     * Drops a ticket out of its match, for example because the player left
     * while the match was still waiting for a server.
     *
     * @return the match the ticket was dropped from, or null if it was in none.
     */
    fun removeTicket(ticketId: UUID): Match? {
        synchronized(lock) {
            val matchId = ticketToMatch.remove(ticketId) ?: return null
            val match = matches[matchId] ?: return null

            val updated = match.copy(ticketIds = match.ticketIds - ticketId)
            matches[matchId] = updated
            logger.info("Ticket {} dropped out of match {}, {} tickets left", ticketId, matchId, updated.ticketIds.size)
            return updated
        }
    }

    /**
     * Removes a finished match.
     *
     * @return the removed match, or null if it was not stored.
     */
    fun remove(id: UUID): Match? {
        synchronized(lock) {
            val match = matches.remove(id) ?: return null
            match.ticketIds.forEach { ticketToMatch.remove(it, id) }
            return match
        }
    }

}

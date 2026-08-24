package net.mythicisland.queue.runtime.repository

import build.buf.gen.mythicisland.queue.v2.MatchState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mythicisland.queue.shared.match.Match
import org.apache.logging.log4j.LogManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MatchRepository {

    private val logger = LogManager.getLogger(MatchRepository::class.java)

    private val mutex = Mutex()
    private val matches = ConcurrentHashMap<UUID, Match>()
    private val ticketToMatch = ConcurrentHashMap<UUID, UUID>()

    fun getMatch(id: UUID): Match? {
        return matches[id]
    }

    fun getMatchByTicket(ticketId: UUID): Match? {
        return ticketToMatch[ticketId]?.let { matches[it] }
    }

    fun getAllMatches(): List<Match> {
        return matches.values.toList()
    }

    fun getAllMatchesByType(queueType: String): List<Match> {
        return matches.values.filter { it.queueType == queueType }
    }

    fun getAllMatchesByState(state: MatchState): List<Match> {
        return matches.values.filter { it.state == state }
    }

    suspend fun addMatch(match: Match) {
        mutex.withLock {
            matches[match.id] = match
            match.ticketIds.forEach { ticketToMatch[it] = match.id }
        }
    }

    suspend fun updateMatch(match: Match): Match? {
        mutex.withLock {
            if (!matches.containsKey(match.id)) return null
            matches[match.id] = match
            return match
        }
    }

    suspend fun removeTicket(ticketId: UUID): Match? {
        mutex.withLock {
            val matchId = ticketToMatch.remove(ticketId) ?: return null
            val match = matches[matchId] ?: return null

            val updated = match.copy(ticketIds = match.ticketIds - ticketId)
            matches[matchId] = updated
            logger.info("Removed ticket {} from match {}", ticketId, matchId)
            return updated
        }
    }

    suspend fun removeMatch(id: UUID): Match? {
        mutex.withLock {
            val match = matches.remove(id) ?: return null
            match.ticketIds.forEach { ticketToMatch.remove(it, id) }
            return match
        }
    }

}

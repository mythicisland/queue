package net.mythicisland.queue.runtime.match

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mythicisland.queue.runtime.state.StateStore
import net.mythicisland.queue.shared.match.Match
import java.util.concurrent.ConcurrentHashMap
import build.buf.gen.mythicisland.queue.v1.Match as MatchProto

class MatchStore(private val state: StateStore) {

    private val mutex = Mutex()
    private val matches = ConcurrentHashMap<String, Match>()

    suspend fun load() = mutex.withLock {
        for (bytes in state.load()) {
            val match = Match.fromProto(MatchProto.parseFrom(bytes))
            matches[match.id] = match
        }
    }

    fun get(id: String): Match? = matches[id]

    fun all(): List<Match> = matches.values.toList()

    suspend fun add(match: Match) = mutex.withLock {
        matches[match.id] = match
        save(match)
    }

    suspend fun remove(id: String): Match? = mutex.withLock {
        val match = matches.remove(id) ?: return@withLock null
        state.delete(id)
        match
    }

    suspend fun replace(previous: Match, current: Match): Boolean = mutex.withLock {
        if (!matches.replace(previous.id, previous, current)) {
            return@withLock false
        }
        save(current)
        true
    }

    suspend fun retryFailedWrites() = mutex.withLock {
        state.retryFailed()
    }

    private fun save(match: Match) = state.put(match.id, match.toProto().toByteArray())

}

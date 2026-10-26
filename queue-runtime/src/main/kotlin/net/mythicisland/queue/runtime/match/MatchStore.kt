package net.mythicisland.queue.runtime.match

import net.mythicisland.queue.shared.match.Match
import java.util.concurrent.ConcurrentHashMap

class MatchStore {

    private val matches = ConcurrentHashMap<String, Match>()

    fun get(id: String): Match? = matches[id]

    fun all(): List<Match> = matches.values.toList()

    fun add(match: Match) {
        matches[match.id] = match
    }

    fun remove(id: String): Match? = matches.remove(id)

    fun replace(previous: Match, current: Match): Boolean = matches.replace(previous.id, previous, current)

}

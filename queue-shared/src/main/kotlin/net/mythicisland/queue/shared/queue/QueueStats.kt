package net.mythicisland.queue.shared.queue

import build.buf.gen.mythicisland.queue.v2.queueStats

data class QueueStats(
    val queueType: String,
    val searchingTickets: Int,
    val searchingPlayers: Int,
    val activeMatches: Int,
) {

    fun toDefinition(): build.buf.gen.mythicisland.queue.v2.QueueStats {
        return queueStats {
            queueType = this@QueueStats.queueType
            searchingTickets = this@QueueStats.searchingTickets
            searchingPlayers = this@QueueStats.searchingPlayers
            activeMatches = this@QueueStats.activeMatches
        }
    }

}

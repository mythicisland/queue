package net.mythicisland.queue.shared.queue

import build.buf.gen.mythicisland.queue.v2.queueStats

/**
 * Very weird metrics for queue types.
 *
 * @param queueType the queue type these numbers belong to.
 * @param searchingTickets the tickets currently searching in this queue type.
 * @param searchingPlayers the players behind those tickets, parties counted fully.
 * @param activeMatches the matches of this queue type that did not finish yet.
 */
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

package net.mythicisland.queue.shared.stats

import build.buf.gen.mythicisland.queue.v1.queueStats
import build.buf.gen.mythicisland.queue.v1.QueueStats as QueueStatsProto

data class QueueStats(
    val queueType: String,
    val searchingTickets: Int,
    val searchingPlayers: Int,
    val activeMatches: Int,
) {

    fun toProto(): QueueStatsProto = queueStats {
        queueType = this@QueueStats.queueType
        searchingTickets = this@QueueStats.searchingTickets
        searchingPlayers = this@QueueStats.searchingPlayers
        activeMatches = this@QueueStats.activeMatches
    }

    companion object {
        fun fromProto(proto: QueueStatsProto): QueueStats =
            QueueStats(proto.queueType, proto.searchingTickets, proto.searchingPlayers, proto.activeMatches)
    }

}

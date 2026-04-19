package net.mythicisland.queue.runtime.repository

import net.mythicisland.queue.db.tables.references.QUEUE_TYPE_ACTIVITY
import net.mythicisland.queue.runtime.database.Database
import org.jooq.impl.DSL
import java.time.LocalDateTime
import java.util.UUID

/**
 * Repository for queue type activity tracking.
 */
class QueueTypeActivityRepository(
    private val db: Database,
) {

    /**
     * Records an enqueue activity for the given players and queue type.
     *
     * @param queueType The queue type name
     * @param playerIds The players that were enqueued
     */
    fun record(queueType: String, playerIds: List<UUID>) {
        db.context.transaction { config ->
            val ctx = config.dsl()
            for (playerId in playerIds) {
                ctx.insertInto(QUEUE_TYPE_ACTIVITY)
                    .set(QUEUE_TYPE_ACTIVITY.QUEUE_TYPE, queueType)
                    .set(QUEUE_TYPE_ACTIVITY.PLAYER_ID, playerId.toString())
                    .execute()
            }
        }
    }

    /**
     * Counts distinct players that enqueued into a specific queue type since the given time.
     *
     * @param queueType The queue type name
     * @param since Only count activity after this timestamp
     * @return The number of distinct players
     */
    fun countDistinctPlayers(queueType: String, since: LocalDateTime): Int {
        return db.context
            .select(DSL.countDistinct(QUEUE_TYPE_ACTIVITY.PLAYER_ID))
            .from(QUEUE_TYPE_ACTIVITY)
            .where(QUEUE_TYPE_ACTIVITY.QUEUE_TYPE.eq(queueType))
            .and(QUEUE_TYPE_ACTIVITY.ENQUEUED_AT.greaterOrEqual(since))
            .fetchOne(0, Int::class.java) ?: 0
    }

    /**
     * Counts distinct players per queue type since the given time.
     *
     * @param since Only count activity after this timestamp
     * @return A map of queue type name to distinct player count
     */
    fun countDistinctPlayersByType(since: LocalDateTime): Map<String, Int> {
        return db.context
            .select(QUEUE_TYPE_ACTIVITY.QUEUE_TYPE, DSL.countDistinct(QUEUE_TYPE_ACTIVITY.PLAYER_ID))
            .from(QUEUE_TYPE_ACTIVITY)
            .where(QUEUE_TYPE_ACTIVITY.ENQUEUED_AT.greaterOrEqual(since))
            .groupBy(QUEUE_TYPE_ACTIVITY.QUEUE_TYPE)
            .fetch()
            .associate { record ->
                val type = record.get(QUEUE_TYPE_ACTIVITY.QUEUE_TYPE) ?: ""
                val count = record.get(1, Int::class.java) ?: 0
                type to count
            }
    }
}
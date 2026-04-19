package net.mythicisland.queue.runtime.repository

import build.buf.gen.mythicisland.queue.v1.QueueStatus
import net.mythicisland.queue.db.tables.references.QUEUES
import net.mythicisland.queue.db.tables.references.QUEUE_PLAYERS
import net.mythicisland.queue.runtime.database.Database
import net.mythicisland.queue.shared.queue.Queue
import java.util.UUID

class QueueDatabaseRepository(
    private val db: Database
) {

    fun save(queue: Queue) {
        db.context.transaction { config ->
            val ctx = config.dsl()

            ctx.insertInto(QUEUES)
                .set(QUEUES.ID, queue.id.toString())
                .set(QUEUES.QUEUE_TYPE, queue.type)
                .set(QUEUES.STATUS, queue.status.number)
                .set(QUEUES.CAPACITY, queue.capacity)
                .set(QUEUES.WAITING_COUNTDOWN_REMAINING, queue.waitingCountdownRemaining)
                .set(QUEUES.COUNTDOWN_REMAINING, queue.countdownRemaining)
                .onDuplicateKeyUpdate()
                .set(QUEUES.QUEUE_TYPE, queue.type)
                .set(QUEUES.STATUS, queue.status.number)
                .set(QUEUES.CAPACITY, queue.capacity)
                .set(QUEUES.WAITING_COUNTDOWN_REMAINING, queue.waitingCountdownRemaining)
                .set(QUEUES.COUNTDOWN_REMAINING, queue.countdownRemaining)
                .execute()

            ctx.deleteFrom(QUEUE_PLAYERS)
                .where(QUEUE_PLAYERS.QUEUE_ID.eq(queue.id.toString()))
                .execute()

            queue.players.forEachIndexed { index, playerId ->
                ctx.insertInto(QUEUE_PLAYERS)
                    .set(QUEUE_PLAYERS.QUEUE_ID, queue.id.toString())
                    .set(QUEUE_PLAYERS.PLAYER_ID, playerId.toString())
                    .set(QUEUE_PLAYERS.POSITION, index)
                    .execute()
            }
        }
    }

    fun delete(queueId: UUID) {
        db.context.transaction { config ->
            val ctx = config.dsl()

            ctx.deleteFrom(QUEUE_PLAYERS)
                .where(QUEUE_PLAYERS.QUEUE_ID.eq(queueId.toString()))
                .execute()

            ctx.deleteFrom(QUEUES)
                .where(QUEUES.ID.eq(queueId.toString()))
                .execute()
        }
    }

    fun loadAll(): List<Queue> {
        val records = db.context.selectFrom(QUEUES).fetch()
        val playersByQueueId = db.context.selectFrom(QUEUE_PLAYERS)
            .orderBy(QUEUE_PLAYERS.QUEUE_ID, QUEUE_PLAYERS.POSITION.asc())
            .fetch()
            .groupBy({ it.queueId.orEmpty() }) { record ->
                record.playerId?.let(UUID::fromString)
            }
            .mapValues { (_, players) -> players.filterNotNull() }

        val now = System.currentTimeMillis()

        return records.mapNotNull { record ->
            val queueId = record.id ?: return@mapNotNull null
            val uuid = UUID.fromString(queueId)

            val statusNumber = record.status ?: 0
            val status = QueueStatus.forNumber(statusNumber) ?: QueueStatus.NOT_ENOUGH_PLAYERS

            Queue(
                id = uuid,
                type = record.queueType ?: return@mapNotNull null,
                status = status,
                players = playersByQueueId[queueId].orEmpty().toMutableList(),
                capacity = record.capacity ?: 0,
            ).also {
                it.waitingCountdownEndsAt = record.waitingCountdownRemaining
                    ?.takeIf { r -> r > 0 }?.let { r -> now + r }
                it.countdownEndsAt = record.countdownRemaining
                    ?.takeIf { r -> r > 0 }?.let { r -> now + r }
            }
        }
    }
}
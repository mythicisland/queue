package net.mythicisland.queue.runtime.queue.persistence

import build.buf.gen.mythicisland.queue.v1.QueueStatus
import net.mythicisland.queue.db.tables.references.QUEUES
import net.mythicisland.queue.db.tables.references.QUEUE_PLAYERS
import net.mythicisland.queue.runtime.database.Database
import net.mythicisland.queue.runtime.queue.Queue
import java.util.UUID

class PersistenceQueueRepository(
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

        return records.mapNotNull { record ->
            val queueId = record.id ?: return@mapNotNull null
            val uuid = UUID.fromString(queueId)

            val playerRecords = db.context.selectFrom(QUEUE_PLAYERS)
                .where(QUEUE_PLAYERS.QUEUE_ID.eq(queueId))
                .orderBy(QUEUE_PLAYERS.POSITION.asc())
                .fetch()

            val players = playerRecords.mapNotNull { it.playerId?.let { id -> UUID.fromString(id) } }

            val statusNumber = record.status ?: 0
            val status = QueueStatus.forNumber(statusNumber) ?: QueueStatus.NOT_ENOUGH_PLAYERS

            Queue(
                id = uuid,
                type = record.queueType ?: return@mapNotNull null,
                status = status,
                players = players.toMutableList(),
                capacity = record.capacity ?: 0,
            ).also {
                it.waitingCountdownRemaining = record.waitingCountdownRemaining ?: 0
                it.countdownRemaining = record.countdownRemaining ?: 0
            }
        }
    }
}
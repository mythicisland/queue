package net.mythicisland.queue.runtime.rating

import build.buf.gen.mythicisland.queue.v1.QueueRating
import build.buf.gen.mythicisland.queue.v1.QueueStats
import build.buf.gen.mythicisland.queue.v1.queueStats
import net.mythicisland.queue.runtime.persistence.QueueTypeActivityRepository
import net.mythicisland.queue.runtime.repository.QueueTypeRepository
import java.time.LocalDateTime

/**
 * Calculates popularity ratings for queue types based on player activity.
 *
 * The rating is derived from two metrics:
 * - **Share**: The percentage of distinct players this type attracted relative to all types (7-day window).
 * - **Trend**: How the last 24h compare to the 7-day daily average, indicating rising or falling popularity.
 *
 * Rating thresholds (by share):
 * - [QueueRating.POPULAR]: > 30%
 * - [QueueRating.GOOD]: > 15%
 * - [QueueRating.MEDIUM]: > 5%
 * - [QueueRating.LOW]: > 2%
 * - [QueueRating.DEAD]: <= 2%
 */
class QueueTypeRatingCalculator(
    private val activityRepository: QueueTypeActivityRepository,
    private val typeRepository: QueueTypeRepository,
) {

    /**
     * Calculates stats for a single queue type.
     *
     * @param queueTypeName The queue type name
     * @return The computed stats, or null if the queue type does not exist
     */
    fun calculate(queueTypeName: String): QueueStats? {
        typeRepository.find(queueTypeName) ?: return null
        return calculateAll().find { it.queueType == queueTypeName }
    }

    /**
     * Calculates stats for all registered queue types.
     *
     * Types with no activity in the last 7 days will still appear with zeroed counts
     * and a [QueueRating.DEAD] rating.
     *
     * @return A list of stats for every registered queue type
     */
    fun calculateAll(): List<QueueStats> {
        val now = LocalDateTime.now()
        val sevenDaysAgo = now.minusDays(7)
        val oneDayAgo = now.minusDays(1)

        val playersByType7d = activityRepository.countDistinctPlayersByType(sevenDaysAgo)
        val playersByType24h = activityRepository.countDistinctPlayersByType(oneDayAgo)
        val totalPlayers7d = playersByType7d.values.sum()

        return typeRepository.getAll().map { type ->
            val players7d = playersByType7d[type.name] ?: 0
            val players24h = playersByType24h[type.name] ?: 0

            val share = if (totalPlayers7d > 0) {
                players7d.toDouble() / totalPlayers7d * 100.0
            } else {
                0.0
            }

            val dailyAverage7d = players7d.toDouble() / 7.0
            val trend = if (dailyAverage7d > 0) {
                players24h.toDouble() / dailyAverage7d * 100.0
            } else {
                0.0
            }

            val rating = calculateRating(share)

            queueStats {
                this.queueType = type.name
                this.rating = rating
                this.totalPlayers7D = players7d
                this.totalPlayers24H = players24h
                this.sharePercent = share
                this.trendPercent = trend
            }
        }
    }

    /**
     * Determines the [QueueRating] based on the share percentage.
     *
     * @param share The percentage share of players relative to all queue types
     * @return The corresponding rating
     */
    private fun calculateRating(share: Double): QueueRating {
        return when {
            share > 30.0 -> QueueRating.POPULAR
            share > 15.0 -> QueueRating.GOOD
            share > 5.0 -> QueueRating.MEDIUM
            share > 2.0 -> QueueRating.LOW
            else -> QueueRating.DEAD
        }
    }
}

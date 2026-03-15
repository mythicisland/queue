package net.mythicisland.queue.runtime.queue.visualizer

import app.simplecloud.api.player.PlayerApi
import build.buf.gen.mythicisland.queue.v1.QueueStatus
import kotlinx.coroutines.future.await
import net.mythicisland.queue.runtime.queue.Queue
import net.mythicisland.queue.runtime.queue.QueueType
import org.apache.logging.log4j.LogManager

class ActionbarVisualizer(
    private val api: PlayerApi
) : QueueVisualizer {

    private val logger = LogManager.getLogger(ActionbarVisualizer::class.java)

    override suspend fun send(queue: Queue, type: QueueType, status: QueueStatus) {
        val text = QueueVisualizer.createQueueText(queue, type, status)

        queue.players.toList().forEach { playerId ->
            try {
                val player = api.get(playerId).await() ?: return@forEach
                player.sendActionBar(text)
            } catch (e: Exception) {
                logger.debug("Failed to send actionbar to player {}: {}", playerId, e.message)
            }
        }
    }
}

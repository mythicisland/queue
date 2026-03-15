package net.mythicisland.queue.runtime.queue.visualizer

import app.simplecloud.api.player.PlayerApi
import build.buf.gen.mythicisland.queue.v1.QueueStatus
import kotlinx.coroutines.future.await
import net.mythicisland.queue.runtime.queue.Queue
import net.mythicisland.queue.runtime.queue.QueueType

class ActionbarVisualizer(
    private val api: PlayerApi
) : QueueVisualizer {

    override suspend fun send(queue: Queue, type: QueueType, status: QueueStatus) {
        queue.players.forEach { player ->
            api.get(player).await().sendActionBar(QueueVisualizer.createQueueText(queue, type, status))
        }

    }

}
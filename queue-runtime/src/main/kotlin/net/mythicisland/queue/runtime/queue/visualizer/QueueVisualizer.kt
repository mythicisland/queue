package net.mythicisland.queue.runtime.queue.visualizer

import build.buf.gen.mythicisland.queue.v1.QueueStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.mythicisland.queue.runtime.queue.Queue
import net.mythicisland.queue.runtime.queue.QueueType

interface QueueVisualizer {

    suspend fun send(queue: Queue, type: QueueType, status: QueueStatus)

    companion object {

        fun createQueueText(
            queue: Queue,
            type: QueueType,
            status: QueueStatus,
        ): Component {
            val raw = type.messages[status] ?: return Component.empty()
            val serverTagResolver = getServerTagResolver(queue)
            val queueTagResolver = QueueTagResolver.get(queue, type)

            return MiniMessage.builder().editTags { tags ->
                if (serverTagResolver != null)
                    tags.resolver(serverTagResolver)
                tags.resolver(queueTagResolver)
            }.build().deserialize(raw)
        }

        private fun getServerTagResolver(queue: Queue): TagResolver? {
            val server = queue.server ?: return null
            return ServerTagResolver.get(server)
        }
    }
}
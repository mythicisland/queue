package net.mythicisland.queue.runtime.visualizer.resolver

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.mythicisland.queue.shared.queue.Queue
import net.mythicisland.queue.shared.queue.QueueType

object QueueTagResolver {

    fun get(queue: Queue, type: QueueType): TagResolver {
        return TagResolver.resolver(
            TagResolver.resolver("queue_type", Tag.inserting(Component.text(queue.type))),
            TagResolver.resolver("queue_id", Tag.inserting(Component.text(queue.id.toString()))),
            TagResolver.resolver("queue_players", Tag.inserting(Component.text(queue.players.size.toString()))),
            TagResolver.resolver("queue_max_capacity", Tag.inserting(Component.text(type.maxCapacity.toString()))),
            TagResolver.resolver("queue_min_capacity", Tag.inserting(Component.text(type.minCapacity.toString()))),
            TagResolver.resolver("queue_countdown_seconds", Tag.inserting(Component.text((queue.countdownRemaining / 1000).toString()))),
            TagResolver.resolver("queue_waiting_countdown_seconds", Tag.inserting(Component.text((queue.waitingCountdownRemaining / 1000).toString())))
        )
    }
}
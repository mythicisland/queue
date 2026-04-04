package net.mythicisland.queue.shared.queue

import net.mythicisland.queue.shared.message.Messages
import net.mythicisland.queue.shared.message.defaultMessages
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class QueueType(
    val name: String = "",
    val group: String = "",
    val maxCapacity: Long = -1L,
    val minCapacity: Long = -1L,
    val waitingCountdownSeconds: Long = 30L,
    val countdownSeconds: Long = 10L,
    val messages: Messages = defaultMessages
) {
    fun toDefinition() : build.buf.gen.mythicisland.queue.v1.QueueType {
        return build.buf.gen.mythicisland.queue.v1.QueueType.newBuilder()
            .setName(name)
            .setGroup(group)
            .setMaxCapacity(maxCapacity.toInt())
            .setMinCapacity(minCapacity.toInt())
            .setWaitingCountdownMillis(waitingCountdownSeconds * 1000)
            .setCountdownMillis(countdownSeconds * 1000)
            .build()

    }

}
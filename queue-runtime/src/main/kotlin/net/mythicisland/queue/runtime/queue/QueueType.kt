package net.mythicisland.queue.runtime.queue

import net.mythicisland.queue.runtime.queue.message.Messages
import net.mythicisland.queue.runtime.queue.message.defaultMessages
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

    companion object {
        fun fromDefinition(definition: build.buf.gen.mythicisland.queue.v1.QueueType): QueueType {
            return QueueType(
                name = definition.name,
                group = definition.group,
                maxCapacity = definition.maxCapacity.toLong(),
                minCapacity = definition.minCapacity.toLong(),
                waitingCountdownSeconds = definition.waitingCountdownMillis / 1000,
                countdownSeconds = definition.countdownMillis / 1000
            )
        }
    }
}
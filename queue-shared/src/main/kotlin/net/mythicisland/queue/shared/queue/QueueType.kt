package net.mythicisland.queue.shared.queue

import build.buf.gen.mythicisland.queue.v2.queueType
import net.mythicisland.common.util.protobuf.secondsToProtobufDuration
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class QueueType(
    val name: String = "",
    val group: String = "",
    val minPlayers: Int = -1,
    val maxPlayers: Int = -1,
    val waitingDurationSeconds: Long = 30L,
    val countdownDurationSeconds: Long = 10L,
) {

    fun toDefinition(): build.buf.gen.mythicisland.queue.v2.QueueType {
        return queueType {
            name = this@QueueType.name
            group = this@QueueType.group
            minPlayers = this@QueueType.minPlayers
            maxPlayers = this@QueueType.maxPlayers
            waitingDuration = this@QueueType.waitingDurationSeconds.secondsToProtobufDuration()
            countdownDuration = this@QueueType.countdownDurationSeconds.secondsToProtobufDuration()
        }
    }

}

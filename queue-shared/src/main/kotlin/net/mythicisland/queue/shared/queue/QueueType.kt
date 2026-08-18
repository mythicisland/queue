package net.mythicisland.queue.shared.queue

import build.buf.gen.mythicisland.queue.v2.queueType
import net.mythicisland.common.util.protobuf.secondsToProtobufDuration
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * The configuration of one queue.
 *
 * @param name the name of this queue type, for example battle.
 * @param group the simplecloud group used to start game servers.
 * @param minPlayers the minimum amount of players needed before a match may start.
 * @param maxPlayers the maximum amount of players a match can hold.
 * @param waitingDurationSeconds how long to wait for more players after minPlayers was reached.
 * @param countdownDurationSeconds how long to count down once the server is ready.
 */
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

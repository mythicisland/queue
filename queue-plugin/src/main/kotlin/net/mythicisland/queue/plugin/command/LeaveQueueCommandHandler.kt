package net.mythicisland.queue.plugin.command

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.MiniMessage
import net.mythicisland.queue.api.QueueApi

/**
 * Handles the /leavequeue command to dequeue a player from their current queue.
 *
 * Usage: /leavequeue
 */
class LeaveQueueCommandHandler(
    private val api: QueueApi
) : SimpleCommand {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val miniMessage = MiniMessage.miniMessage()

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        if (source !is Player) {
            source.sendMessage(miniMessage.deserialize("<color:#dc2626>This command can only be used by players!"))
            return
        }

        scope.launch {
            try {
                api.player().dequeue(source.uniqueId).await()
            } catch (_: Exception) {
                // Messages are sent by the runtime
            }
        }
    }

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
        return invocation.source().hasPermission("mythicisland.queue.command.dequeue")
    }
}
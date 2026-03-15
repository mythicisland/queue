package net.mythicisland.queue.plugin.command

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
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

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        if (source !is Player) {
            source.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED))
            return
        }

        scope.launch {
            try {
                api.player().dequeue(source.uniqueId).await()
                source.sendMessage(Component.text("You have left the queue.", NamedTextColor.GREEN))
            } catch (e: StatusRuntimeException) {
                source.sendMessage(Component.text(e.status.description ?: "Failed to leave queue.", NamedTextColor.RED))
            } catch (e: Exception) {
                source.sendMessage(Component.text("An error occurred while leaving the queue.", NamedTextColor.RED))
            }
        }
    }

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
        return invocation.source().hasPermission("mythicisland.queue.command.dequeue")
    }
}

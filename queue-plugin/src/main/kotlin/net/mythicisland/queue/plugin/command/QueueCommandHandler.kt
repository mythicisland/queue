package net.mythicisland.queue.plugin.command

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.MiniMessage
import net.mythicisland.queue.api.QueueApi

/**
 * Handles the /queue command to enqueue a player into a queue type.
 *
 * Usage: /queue <type>
 */
class QueueCommandHandler(
    private val api: QueueApi
) : SimpleCommand {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val miniMessage = MiniMessage.miniMessage()

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        if (source !is Player) {
            source.sendMessage(miniMessage.deserialize("<color:#dc2626>This Command can only used by players!"))
            return
        }

        val args = invocation.arguments()
        if (args.isEmpty()) {
            source.sendMessage(miniMessage.deserialize("<color:#ffffff>Usage: /queue <type>"))
            return
        }

        val type = args[0]

        scope.launch {
            try {
                api.player().enqueue(type, source.uniqueId).await()
                source.sendMessage(miniMessage.deserialize("<color:#22c55e>You have been joined the Queue"))
            } catch (e: StatusRuntimeException) {
                source.sendMessage(miniMessage.deserialize("<color:#dc2626>Failed to join the Queue, Please contact an Administrator about this!"))
            } catch (e: Exception) {
                source.sendMessage(miniMessage.deserialize("<color:#dc2626>Failed to join the Queue, Please contact an Administrator about this!"))
            }
        }
    }

    override fun suggest(invocation: SimpleCommand.Invocation): List<String> {
        if (invocation.arguments().size > 1) return emptyList()

        val types = try {
            api.data().getAllQueueTypes().get()
                .queueTypesList.map { it.name }
        } catch (e: Exception) {
            return emptyList()
        }

        val prefix = invocation.arguments().firstOrNull()?.lowercase() ?: ""
        return types.filter { it.lowercase().startsWith(prefix) }
    }

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
        return invocation.source().hasPermission("mythicisland.queue.command.enqueue")
    }
}

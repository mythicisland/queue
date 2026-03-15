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
 * Handles the /queue command to enqueue a player into a queue type.
 *
 * Usage: /queue <type>
 */
class QueueCommandHandler(
    private val api: QueueApi
) : SimpleCommand {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        if (source !is Player) {
            source.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED))
            return
        }

        val args = invocation.arguments()
        if (args.isEmpty()) {
            source.sendMessage(Component.text("Usage: /queue <type>", NamedTextColor.RED))
            return
        }

        val type = args[0]

        scope.launch {
            try {
                api.player().enqueue(type, source.uniqueId).await()
                source.sendMessage(Component.text("You have been added to the $type queue.", NamedTextColor.GREEN))
            } catch (e: StatusRuntimeException) {
                source.sendMessage(Component.text(e.status.description ?: "Failed to join queue.", NamedTextColor.RED))
            } catch (e: Exception) {
                source.sendMessage(Component.text("An error occurred while joining the queue.", NamedTextColor.RED))
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

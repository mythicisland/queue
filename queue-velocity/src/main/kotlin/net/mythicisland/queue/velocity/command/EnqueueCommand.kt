package net.mythicisland.queue.velocity.command

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.mythicisland.queue.api.QueueApi
import net.mythicisland.queue.api.QueueException
import net.mythicisland.queue.velocity.Colors
import net.mythicisland.queue.velocity.ticket.TicketTracker
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class EnqueueCommand(
    private val api: QueueApi,
    private val tracker: TicketTracker,
    private val scope: CoroutineScope,
) : SimpleCommand {

    private val logger = LoggerFactory.getLogger(EnqueueCommand::class.java)

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        if (source !is Player) {
            source.sendMessage(Component.text("Only players can use this command.", Colors.ERROR))
            return
        }

        val arguments = invocation.arguments()
        if (arguments.size != 1) {
            source.sendMessage(Component.text("Usage: /enqueue <queue>", Colors.ERROR))
            return
        }

        val queueType = arguments[0]
        scope.launch {
            try {
                tracker.track(api.ticket().createTicket(source.uniqueId, queueType))
                source.sendMessage(Component.text("You joined $queueType.", Colors.SUCCESS))
            } catch (e: CancellationException) {
                throw e
            } catch (e: QueueException) {
                source.sendMessage(Component.text(errorMessage(e, queueType), Colors.ERROR))
            } catch (e: Exception) {
                logger.error("Enqueue of {} failed", source.uniqueId, e)
                source.sendMessage(Component.text("Something went wrong, please try again.", Colors.ERROR))
            }
        }
    }

    override fun suggestAsync(invocation: SimpleCommand.Invocation): CompletableFuture<List<String>> {
        val arguments = invocation.arguments()
        if (arguments.size > 1) {
            return CompletableFuture.completedFuture(emptyList())
        }

        val prefix = arguments.firstOrNull()?.lowercase() ?: ""
        return api.data().listQueueTypesAsync()
            .thenApply { types -> types.map { type -> type.name }.filter { name -> name.lowercase().startsWith(prefix) } }
            .exceptionally { emptyList() }
    }

    private fun errorMessage(e: QueueException, queueType: String): String = when (e.code) {
        QueueException.Code.ALREADY_EXISTS -> "You are already in a queue."
        QueueException.Code.INVALID_ARGUMENT -> "Unknown queue $queueType."
        QueueException.Code.UNAVAILABLE -> "The queue is not available right now."
        else -> "Something went wrong, please try again."
    }

}

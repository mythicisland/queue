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

class DequeueCommand(
    private val api: QueueApi,
    private val tracker: TicketTracker,
    private val scope: CoroutineScope,
) : SimpleCommand {

    private val logger = LoggerFactory.getLogger(DequeueCommand::class.java)

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        if (source !is Player) {
            source.sendMessage(Component.text("Only players can use this command.", Colors.ERROR))
            return
        }

        scope.launch {
            try {
                api.ticket().deleteTicketByPlayer(source.uniqueId)
                tracker.untrackPlayer(source.uniqueId)
                source.sendMessage(Component.text("You left the queue.", Colors.SUCCESS))
            } catch (e: CancellationException) {
                throw e
            } catch (e: QueueException) {
                source.sendMessage(Component.text(errorMessage(e), Colors.ERROR))
            } catch (e: Exception) {
                logger.error("Dequeue of {} failed", source.uniqueId, e)
                source.sendMessage(Component.text("Something went wrong, please try again.", Colors.ERROR))
            }
        }
    }

    private fun errorMessage(e: QueueException): String = when (e.code) {
        QueueException.Code.NOT_FOUND -> "You are not in a queue."
        QueueException.Code.UNAVAILABLE -> "The queue is not available right now."
        else -> "Something went wrong, please try again."
    }

}

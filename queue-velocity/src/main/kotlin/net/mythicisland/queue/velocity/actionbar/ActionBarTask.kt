package net.mythicisland.queue.velocity.actionbar

import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.mythicisland.queue.velocity.Colors
import net.mythicisland.queue.velocity.ticket.TicketTracker
import org.slf4j.LoggerFactory
import java.time.Instant

class ActionBarTask(
    private val server: ProxyServer,
    private val tracker: TicketTracker,
) : Runnable {

    private val logger = LoggerFactory.getLogger(ActionBarTask::class.java)

    override fun run() {
        val now = Instant.now()
        for ((playerId, ticket) in tracker.tickets()) {
            try {
                val player = server.getPlayer(playerId).orElse(null) ?: continue
                player.sendActionBar(render(ActionBarStatus.of(ticket, now)))
            } catch (e: Exception) {
                logger.warn("Sending action bar to {} failed", playerId, e)
            }
        }
    }

    private fun render(status: ActionBarStatus): Component = when (status) {
        is ActionBarStatus.Searching -> Component.text().color(Colors.TEXT)
            .append(Component.text("Searching for "))
            .append(Component.text(status.queueTypes.joinToString(", "), Colors.VALUE))
            .append(Component.text(" · "))
            .append(Component.text(ActionBarStatus.formatElapsed(status.elapsed), Colors.VALUE))
            .build()

        ActionBarStatus.Matched -> Component.text("Match found, waiting for a server", Colors.TEXT)

        is ActionBarStatus.Assigned -> Component.text().color(Colors.TEXT)
            .append(Component.text("Sending you to "))
            .append(Component.text(status.serverName, Colors.VALUE))
            .append(Component.text(" in "))
            .append(Component.text(ActionBarStatus.formatRemaining(status.remaining), Colors.VALUE))
            .build()
    }

}

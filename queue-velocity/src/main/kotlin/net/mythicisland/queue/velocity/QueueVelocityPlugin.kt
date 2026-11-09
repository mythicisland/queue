package net.mythicisland.queue.velocity

import com.google.inject.Inject
import com.velocitypowered.api.command.Command
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.*
import net.mythicisland.queue.api.QueueApi
import net.mythicisland.queue.api.event.Subscription
import net.mythicisland.queue.velocity.actionbar.ActionBarTask
import net.mythicisland.queue.velocity.command.DequeueCommand
import net.mythicisland.queue.velocity.command.EnqueueCommand
import net.mythicisland.queue.velocity.ticket.TicketTracker
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

@Plugin(
    id = "queue",
    name = "queue",
    version = VERSION,
    description = "Basic lobby side implementation of queue",
    url = "https://github.com/mythicisland/queue",
)
class QueueVelocityPlugin @Inject constructor(private val server: ProxyServer) {

    private val logger = LoggerFactory.getLogger(QueueVelocityPlugin::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val api = QueueApi.create()
    private val tracker = TicketTracker(server)
    private val subscriptions = CopyOnWriteArrayList<Subscription>()

    @Subscribe
    fun onInitialize(event: ProxyInitializeEvent) {
        subscriptions.addAll(tracker.subscribe(api.event().ticket()))
        server.eventManager.register(this, tracker)

        scope.launch {
            try {
                tracker.load(api.data().listTickets())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Loading tickets failed", e)
            }
        }

        register("enqueue", EnqueueCommand(api, tracker, scope))
        register("dequeue", DequeueCommand(api, tracker, scope))

        server.scheduler.buildTask(this, ActionBarTask(server, tracker))
            .repeat(1, TimeUnit.SECONDS)
            .schedule()
    }

    @Subscribe
    fun onShutdown(event: ProxyShutdownEvent) {
        subscriptions.forEach { it.unsubscribe() }
        scope.cancel()
        api.close()
    }

    private fun register(name: String, command: Command) {
        val commandManager = server.commandManager
        commandManager.register(commandManager.metaBuilder(name).plugin(this).build(), command)
    }

}

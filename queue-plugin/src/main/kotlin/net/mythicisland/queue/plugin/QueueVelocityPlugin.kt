package net.mythicisland.queue.plugin

import com.google.inject.Inject
import com.velocitypowered.api.command.CommandManager
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import net.mythicisland.queue.api.QueueApi
import net.mythicisland.queue.api.QueueApiOptions
import net.mythicisland.queue.plugin.command.LeaveQueueCommandHandler
import net.mythicisland.queue.plugin.command.QueueCommandHandler
import net.mythicisland.queue.plugin.config.QueueConfig
import net.mythicisland.queue.plugin.config.YamlConfig
import org.slf4j.LoggerFactory
import java.nio.file.Path

@Plugin(
    id = "mythicisland-queue",
    name = "mythicisland-queue",
    version = "1.0.0",
    authors = ["xXJanisXx"],
    url = "https://github.com/mythicisland/queue"
)
class QueueVelocityPlugin @Inject constructor(
    @DataDirectory val dataDirectory: Path,
    private val server: ProxyServer,
) {

    private val logger = LoggerFactory.getLogger(QueueVelocityPlugin::class.java)

    private val config = YamlConfig(dataDirectory.toString())
    private val queueConfig = config.load<QueueConfig>("config")

    private val api = connectToQueue()

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        logger.info("Initializing mythicisland-queue...")

        logger.info("Loading config...")
        config.save("config", queueConfig)

        logger.info("Registering commands...")
        registerCommands(server.commandManager)

        logger.info("mythicisland-queue initialized")
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        logger.info("Shutting down mythicisland-queue...")

        logger.info("Closing config...")
        config.close()

        logger.info("Closing queue api connection...")
        api.close()
    }

    private fun registerCommands(commandManager: CommandManager) {
        commandManager.register(
            commandManager.metaBuilder("queue").plugin(this).build(),
            QueueCommandHandler(api)
        )
        commandManager.register(
            commandManager.metaBuilder("leavequeue").plugin(this).build(),
            LeaveQueueCommandHandler(api)
        )
    }

    private fun connectToQueue(): QueueApi {
        try {
            logger.info("Connecting to queue...")
            val config = queueConfig.get().queue

            val api = QueueApi.create(
                QueueApiOptions.builder()
                    .natsUrl(config.natsUrl)
                    .natsUser(config.natsUser)
                    .natsSecret(config.natsSecret)
                    .grpcPort(config.grpcPort)
                    .grpcHost(config.grpcHost)
                    .natsFailoverReconnectAfter(config.natsFailoverReconnectAfter)
                    .build()
            )

            logger.info("Successfully connected to queue!")
            return api
        } catch (e: Exception) {
            logger.error("Failed to connect to queue!", e)
            throw e
        }
    }
}

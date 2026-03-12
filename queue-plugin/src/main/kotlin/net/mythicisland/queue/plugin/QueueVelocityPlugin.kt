package net.mythicisland.queue.plugin

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
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

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        config.save("config", queueConfig)
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        config.close()
    }

}
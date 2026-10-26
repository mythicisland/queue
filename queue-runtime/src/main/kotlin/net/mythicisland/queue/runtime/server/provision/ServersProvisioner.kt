package net.mythicisland.queue.runtime.server.provision

import app.simplecloud.api.CloudApi
import app.simplecloud.api.group.Group
import kotlinx.coroutines.future.await
import net.mythicisland.queue.runtime.server.ServerRegistry
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory

class ServersProvisioner(
    private val api: CloudApi,
    private val registry: ServerRegistry,
) : ServerProvisioner {

    private val logger = LoggerFactory.getLogger(ServersProvisioner::class)

    override suspend fun provision(group: Group, servers: Int) {
        val queued = api.group().serverStartQueue.await().findByServerGroupId(group.serverGroupId)?.queuedStarts ?: 0
        val missing = servers - queued - registry.pending(group.name)
        if (missing <= 0) {
            return
        }

        repeat(missing) {
            api.group().requestServerStart(group.serverGroupId).await()
        }
        logger.info("Requested server starts", Field("group", group.name), Field("count", missing))
    }

}

package net.mythicisland.queue.runtime

import app.simplecloud.api.CloudApi
import app.simplecloud.api.CloudApiOptions
import app.simplecloud.api.cache.CacheConfig
import app.simplecloud.api.cache.EntityCacheConfig
import app.simplecloud.api.cache.EntityType
import kotlinx.coroutines.*
import net.mythicisland.queue.runtime.launcher.QueueStartCommand
import net.mythicisland.queue.runtime.metrics.Telemetry
import net.mythicisland.queue.runtime.nats.NatsClient
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import org.apache.logging.log4j.LogManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class QueueRuntime(private val args: QueueStartCommand) {

    private val logger = LoggerFactory.getLogger(QueueRuntime::class)

    suspend fun start() {
        logger.info("Starting queue runtime")

        val telemetry = Telemetry.create(args.instanceId)
        val nats = NatsClient.init(args.natsUrl, args.natsUser, args.natsSecret)
        val api = connectToController()
        val node = QueueNode(args, nats, api, telemetry.otel)

        try {
            node.start()
        } catch (e: Exception) {
            logger.error("Failed to start queue runtime", Field.error(e))
            stop(node, api, nats, telemetry)
            throw e
        }

        val stopped = CountDownLatch(1)
        suspendCancellableCoroutine { continuation ->
            Runtime.getRuntime().addShutdownHook(Thread {
                continuation.resume(Unit)
                stopped.await(30L, TimeUnit.SECONDS)
            })
        }
        stop(node, api, nats, telemetry)
        stopped.countDown()
    }

    private suspend fun stop(node: QueueNode, api: CloudApi, nats: NatsClient, telemetry: Telemetry) {
        logger.info("Shutting down queue runtime")

        node.shutdown()
        api.close()
        nats.close()
        telemetry.shutdown()

        logger.info("Queue runtime stopped")
        LogManager.shutdown()
    }

    private fun connectToController(): CloudApi {
        logger.info("Connecting to SimpleCloud controller", Field("url", args.controllerUrl))
        return CloudApi.create(
            CloudApiOptions.builder()
                .controllerUrl(args.controllerUrl)
                .natsUrl(args.controllerNatsUrl)
                .networkId(args.networkId)
                .networkSecret(args.networkSecret)
                .cache(CacheConfig.builder()
                    .entityConfig(EntityType.SERVER, EntityCacheConfig.builder().enabled(false).build())
                    .build())
                .build()
        )
    }

}

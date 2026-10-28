package net.mythicisland.queue.runtime

import app.simplecloud.api.CloudApi
import app.simplecloud.api.CloudApiOptions
import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.*
import net.mythicisland.queue.runtime.launcher.QueueStartCommand
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

        val nc = connectToNats()
        val api = connectToController()
        val node = QueueNode(args, nc, api)

        try {
            node.start()
        } catch (e: Exception) {
            logger.error("Failed to start queue runtime", Field.error(e))
            stop(node, api, nc)
            throw e
        }

        val stopped = CountDownLatch(1)
        awaitShutdown(stopped)
        stop(node, api, nc)
        stopped.countDown()
    }

    private suspend fun awaitShutdown(stopped: CountDownLatch) {
        suspendCancellableCoroutine { continuation ->
            Runtime.getRuntime().addShutdownHook(shutdownHook(continuation, stopped))
        }
    }

    private fun shutdownHook(continuation: CancellableContinuation<Unit>, stopped: CountDownLatch): Thread = Thread {
        continuation.resume(Unit)
        stopped.await(30L, TimeUnit.SECONDS)
    }

    private suspend fun stop(node: QueueNode, api: CloudApi, nc: Connection) {
        logger.info("Shutting down queue runtime")

        node.shutdown()
        api.close()
        nc.close()

        logger.info("Queue runtime stopped")
        LogManager.shutdown()
    }

    private fun connectToNats(): Connection {
        val options = Options.builder()
            .server(args.natsUrl)
            .userInfo(args.natsUser, args.natsSecret)
            .maxReconnects(-1)

        logger.info("Connecting to NATS", Field("url", args.natsUrl))
        return Nats.connect(options.build())
    }

    private fun connectToController(): CloudApi {
        logger.info("Connecting to SimpleCloud controller", Field("url", args.controllerUrl))
        return CloudApi.create(
            CloudApiOptions.builder()
                .controllerUrl(args.controllerUrl)
                .natsUrl(args.controllerNatsUrl)
                .networkId(args.networkId)
                .networkSecret(args.networkSecret)
                .build()
        )
    }

}

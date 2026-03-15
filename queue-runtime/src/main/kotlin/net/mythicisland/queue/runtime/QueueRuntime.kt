package net.mythicisland.queue.runtime

import app.simplecloud.api.CloudApi
import app.simplecloud.api.CloudApiOptions
import io.nats.client.Connection
import kotlinx.coroutines.suspendCancellableCoroutine
import net.mythicisland.queue.runtime.config.MessageConfig
import net.mythicisland.queue.runtime.config.YamlConfig
import net.mythicisland.queue.runtime.launcher.QueueStartCommand
import net.mythicisland.queue.runtime.nats.NatsConnectionHandler
import net.mythicisland.queue.runtime.nats.NatsErrorListener
import net.mythicisland.queue.runtime.nats.NatsFailoverConnectionManager
import net.mythicisland.queue.runtime.queue.reconciler.QueueStatusReconciler
import net.mythicisland.queue.runtime.queue.repository.QueueRepository
import net.mythicisland.queue.runtime.queue.repository.QueueTypeRepository
import net.mythicisland.queue.runtime.queue.server.ServerFinder
import net.mythicisland.queue.runtime.queue.visualizer.ActionbarVisualizer
import org.apache.logging.log4j.LogManager

class QueueRuntime(
    private val args: QueueStartCommand
) {
    private val logger = LogManager.getLogger(QueueRuntime::class.java)

    private val config = YamlConfig(args.configPath.toString())
    private val messages = config.load<MessageConfig>("messages")

    private val api = connectToController()

    private val natsConnectionHandler = NatsConnectionHandler()
    private val natsErrorListener = NatsErrorListener()
    private val manager = createNatsConnectionManager()

    private var natsConnection: Connection? = null

    private val queueTypeRepository = QueueTypeRepository
    private val queueRepository = QueueRepository(queueTypeRepository, api.player())
    private val finder = ServerFinder(api, queueTypeRepository)
    private val visualizer = ActionbarVisualizer(api.player())
    private val reconciler = QueueStatusReconciler(
        queueRepository,
        queueTypeRepository,
        api.event(),
        api.player(),
        finder,
        visualizer
    )

    suspend fun start() {
        logger.info("Starting QueueRuntime...")

        logger.info("Loading queue messages...")
        config.save("messages", messages)

        connectNats()

        logger.info("Setting up queue reconciler...")
        queueRepository.setReconciler(reconciler)

        logger.info("Starting queue reconciler...")
        reconciler.startPeriodicReconciliation()
        reconciler.startCountdownReconciliation()
        reconciler.startWaitingCountdownReconciliation()
        reconciler.registerServerRegistrationSubscriber()

        logger.info("Friends started successfully")

        suspendCancellableCoroutine<Unit> { continuation ->
            Runtime.getRuntime().addShutdownHook(Thread {
                manager.shutdown()
                config.close()
                continuation.resume(Unit) { cause, _, _ ->
                    logger.info("runtime shutdown due to: $cause")
                }
            })
        }
    }

    private fun connectToController(): CloudApi {
        try {
            logger.info("Connecting to your Network...")
            val api = CloudApi.create(
                CloudApiOptions.builder()
                    .networkId(args.networkId)
                    .networkSecret(args.networkSecret)
                    .controllerUrl(args.controllerUrl)
                    .natsUrl(args.controllerNatsUrl)
                    .build()
            )
            logger.info("Successfully connected to your Network")
            logger.info("Network ID: {}", api.networkId)
            return api
        } catch (e: Exception) {
            logger.error("Failed to connect to your Network", e)
            throw e
        }
    }

    private fun connectNats() {
        try {
            logger.info("Connecting to NATS...")
            logger.info("NATS failover full reconnect timeout: {}", args.natsFailoverReconnectAfter)

            natsConnection = manager.connection()
            logger.info("Successfully connected to NATS")
        } catch (e: Exception) {
            logger.error("Failed to connect to NATS", e)
            throw e
        }
    }

    private fun createNatsConnectionManager(): NatsFailoverConnectionManager {
        return NatsFailoverConnectionManager(
            natsUrl = args.natsUrl,
            networkId = args.natsUser,
            networkSecret = args.natsSecret,
            errorListener = natsErrorListener,
            connectionHandler = natsConnectionHandler,
            failoverReconnectAfter = args.natsFailoverReconnectAfter,
        )
    }
}
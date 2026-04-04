package net.mythicisland.queue.runtime

import app.simplecloud.api.CloudApi
import app.simplecloud.api.CloudApiOptions
import io.grpc.Server
import io.grpc.ServerBuilder
import io.nats.client.Connection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import net.mythicisland.queue.runtime.database.DatabaseFactory
import net.mythicisland.queue.runtime.launcher.QueueStartCommand
import net.mythicisland.queue.runtime.nats.NatsConnectionHandler
import net.mythicisland.queue.runtime.nats.NatsErrorListener
import net.mythicisland.queue.runtime.nats.NatsFailoverConnectionManager
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.persistence.PersistenceQueueRepository
import net.mythicisland.queue.runtime.reconciler.QueueStatusReconciler
import net.mythicisland.queue.runtime.repository.QueueRepository
import net.mythicisland.queue.runtime.repository.QueueTypeRepository
import net.mythicisland.queue.runtime.server.ServerFinder
import net.mythicisland.queue.runtime.service.QueueDataService
import net.mythicisland.queue.runtime.service.QueueService
import net.mythicisland.queue.runtime.visualizer.ActionbarVisualizer
import org.apache.logging.log4j.LogManager

class QueueRuntime(
    private val args: QueueStartCommand
) {
    private val logger = LogManager.getLogger(QueueRuntime::class.java)

    private val api = connectToController()

    private val natsConnectionHandler = NatsConnectionHandler()
    private val natsErrorListener = NatsErrorListener()
    private val manager = createNatsConnectionManager()

    private var natsConnection: Connection? = null

    private val database = DatabaseFactory.createDatabase(args.databaseUrl)
    private val queueTypeRepository = QueueTypeRepository
    private val persistenceQueueRepository = PersistenceQueueRepository(database)
    private val queueRepository = QueueRepository(queueTypeRepository, persistenceQueueRepository)
    private val finder = ServerFinder(api, queueTypeRepository)
    private val visualizer = ActionbarVisualizer(api.player())
    private val eventPublisher = EventPublisher(manager.connection())
    private val reconciler = QueueStatusReconciler(
        queueRepository,
        queueTypeRepository,
        api.event(),
        api.player(),
        finder,
        visualizer,
        eventPublisher,
    )

    suspend fun start() {
        logger.info("Starting QueueRuntime...")

        logger.info("Loading queue types...")
        queueTypeRepository.load()

        connectNats()

        database.setup()

        logger.info("Loading queues from database...")
        queueRepository.loadFromDatabase()

        logger.info("Setting up queue repository...")
        queueRepository.setReconciler(reconciler)
        queueRepository.setEventPublisher(eventPublisher)

        logger.info("Starting queue reconciler...")
        reconciler.start()

        val server = createGrpcServer()
        startGrpcServer(server)

        suspendCancellableCoroutine { continuation ->
            Runtime.getRuntime().addShutdownHook(Thread {
                logger.info("Shutting down QueueRuntime...")
                runBlocking { shutdown() }
                server.shutdown()
                continuation.resume(Unit) { cause, _, _ ->
                    logger.info("Runtime shutdown due to: $cause")
                }
            })
        }
    }

    private suspend fun shutdown() {
        val queues = queueRepository.getAllQueues()
        if (queues.isNotEmpty()) {
            logger.info("Persisting {} active queues for restart recovery...", queues.size)
            for (queue in queues) {
                queue.server?.let { server ->
                    logger.info("Freeing server {} from queue {}", server.serverId, queue.id)
                    try {
                        finder.freeServer(server)
                    } catch (e: Exception) {
                        logger.warn("Failed to free server {} during shutdown", server.serverId, e)
                    }
                }
            }
        }

        reconciler.shutdown()
        manager.shutdown()
        logger.info("QueueRuntime shutdown complete")
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

    private fun startGrpcServer(server: Server) {
        logger.info("Starting gRPC server on port {}...", args.grpcPort)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                server.start()
                logger.info("gRPC server started on port {}", args.grpcPort)
                server.awaitTermination()
                logger.info("Queue started successfully")
            } catch (e: Exception) {
                logger.error("Error in gRPC server", e)
                throw e
            }
        }
    }

    private fun createGrpcServer(): Server {
        return ServerBuilder.forPort(args.grpcPort)
            .addService(QueueService(queueRepository))
            .addService(QueueDataService(queueRepository, queueTypeRepository))
            .build()
    }

    private fun createNatsConnectionManager(): NatsFailoverConnectionManager {
        return NatsFailoverConnectionManager(
            natsUrl = args.natsUrl,
            natsUser = args.natsUser,
            natsSecret = args.natsSecret,
            errorListener = natsErrorListener,
            connectionHandler = natsConnectionHandler,
            failoverReconnectAfter = args.natsFailoverReconnectAfter,
        )
    }
}
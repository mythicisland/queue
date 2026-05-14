package net.mythicisland.queue.runtime

import app.simplecloud.api.CloudApi
import app.simplecloud.api.CloudApiOptions
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import net.mythicisland.queue.runtime.launcher.QueueStartCommand
import net.mythicisland.queue.runtime.nats.NatsConnectionHandler
import net.mythicisland.queue.runtime.nats.NatsErrorListener
import net.mythicisland.queue.runtime.nats.NatsFailoverConnectionManager
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.reconciler.QueueReconciler
import net.mythicisland.queue.runtime.repository.QueueRepository
import net.mythicisland.queue.runtime.repository.QueueTypeRepository
import net.mythicisland.queue.runtime.server.ServerFinder
import net.mythicisland.queue.runtime.service.QueueDataService
import net.mythicisland.queue.runtime.service.QueueService
import org.apache.logging.log4j.LogManager

class QueueRuntime(
    private val args: QueueStartCommand
) {
    private val logger = LogManager.getLogger(QueueRuntime::class.java)

    private val natsConnectionHandler = NatsConnectionHandler()
    private val natsErrorListener = NatsErrorListener()
    private val manager = createNatsConnectionManager()

    private val eventPublisher = EventPublisher(manager.connection())

    private val queueTypeRepository = QueueTypeRepository(args.typesPath)
    private val queueRepository = QueueRepository(queueTypeRepository, eventPublisher)

    suspend fun start() {
        logger.info("Starting QueueRuntime...")

        logger.info("Loading queue types...")
        queueTypeRepository.load()

        val api = connectToController()
        val finder = ServerFinder(api, queueTypeRepository)
        
        val reconciler = QueueReconciler(
            queueRepository,
            queueTypeRepository,
            api,
            finder,
            eventPublisher,
        )

        queueRepository.setReconciler(reconciler)

        logger.info("Setting up queue reconciler...")
        reconciler.start()

        val server = createGrpcServer()
        startGrpcServer(server)

        suspendCancellableCoroutine { continuation ->
            Runtime.getRuntime().addShutdownHook(Thread {
                logger.info("Shutting down QueueRuntime...")
                runBlocking {
                    val queues = queueRepository.getAllQueues()
                    if (queues.isNotEmpty()) {
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
                }
                server.shutdown()
                continuation.resume(Unit) { cause, _, _ ->
                    logger.info("Runtime shutdown due to: $cause")
                }
            })
        }
    }

    private fun startGrpcServer(server: Server) {
        logger.info("Starting gRPC server on port {}...", args.grpcPort)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                server.start()
                logger.info("gRPC server started on port {}", args.grpcPort)
                server.awaitTermination()
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
        return NatsFailoverConnectionManager(args.natsUrl, args.natsUser, args.natsSecret, natsErrorListener, natsConnectionHandler, args.natsFailoverReconnectAfter)
    }

    private fun connectToController(): CloudApi {
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
    }
}

package net.mythicisland.queue.runtime

import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import net.mythicisland.moonrise.common.MoonriseCommon
import net.mythicisland.queue.runtime.launcher.QueueStartCommand
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

    private val manager = MoonriseCommon.createNatsConnectionManager(args.natsUrl, args.natsUser, args.natsSecret)

    private val eventPublisher = EventPublisher(manager.connection())
    private val queueTypeRepository = QueueTypeRepository(args.typesPath)
    private val queueRepository = QueueRepository(queueTypeRepository, eventPublisher)

    suspend fun start() {
        logger.info("Starting QueueRuntime...")

        logger.info("Loading queue types...")
        queueTypeRepository.load()

        val api = MoonriseCommon.connectToController(
            args.networkId,
            args.networkSecret,
            args.controllerUrl,
            args.controllerNatsUrl
        )
        val finder = ServerFinder(api, queueTypeRepository)
        
        val reconciler = QueueReconciler(
            queueRepository,
            queueTypeRepository,
            api,
            finder,
            eventPublisher,
        )

        queueRepository.setReconciler(reconciler)
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
                                finder.freeServer(server)
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
}

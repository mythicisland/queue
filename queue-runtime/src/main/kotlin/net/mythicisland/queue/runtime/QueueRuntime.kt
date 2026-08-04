package net.mythicisland.queue.runtime

import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import net.mythicisland.moonrise.common.Moonrise
import net.mythicisland.moonrise.common.auth.AuthInterceptor
import net.mythicisland.moonrise.common.auth.AuthSecret
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.launcher.QueueStartCommand
import net.mythicisland.queue.runtime.match.MatchReconciler
import net.mythicisland.queue.runtime.match.Matchmaker
import net.mythicisland.queue.runtime.repository.MatchRepository
import net.mythicisland.queue.runtime.repository.QueueTypeRepository
import net.mythicisland.queue.runtime.server.ServerAllocator
import net.mythicisland.queue.runtime.service.QueueDataService
import net.mythicisland.queue.runtime.service.TicketService
import net.mythicisland.queue.runtime.ticket.TicketPool
import net.mythicisland.queue.runtime.ticket.TicketStore
import org.apache.logging.log4j.LogManager

class QueueRuntime(
    private val args: QueueStartCommand
) {
    private val logger = LogManager.getLogger(QueueRuntime::class.java)

    private val manager = Moonrise.createNatsConnectionManager(args.natsUrl, args.natsUser, args.natsSecret)

    private val eventPublisher = EventPublisher(manager.connection())
    private val queueTypeRepository = QueueTypeRepository(args.typesPath)
    private val matchRepository = MatchRepository()
    private val ticketStore = TicketStore()
    private val ticketPool = TicketPool(ticketStore)

    suspend fun start() {
        logger.info("Starting Queue Service...")

        logger.info("Loading queue types...")
        val types = queueTypeRepository.load()
        logger.info("Loaded {} queue types: {}", types.size, types.map { it.name })

        val api = Moonrise.connectToController(args.networkId, args.networkSecret, args.controllerUrl, args.controllerNatsUrl)
        val allocator = ServerAllocator(api)

        val matchmaker = Matchmaker(ticketStore, ticketPool, matchRepository, queueTypeRepository, eventPublisher)
        val reconciler = MatchReconciler(ticketStore, matchRepository, queueTypeRepository, allocator, api, eventPublisher)

        matchmaker.start()
        reconciler.start()

        val server = createGrpcServer()
        startGrpcServer(server)

        suspendCancellableCoroutine { continuation ->
            Runtime.getRuntime().addShutdownHook(Thread {
                logger.info("Shutting down Queue...")
                runBlocking {
                    // Free the servers of matches that never made it to a transfer.
                    matchRepository.getAll().forEach { allocator.release(it) }

                    matchmaker.shutdown()
                    reconciler.shutdown()
                    queueTypeRepository.close()
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
        logger.info("Loading auth secret from {}...", args.authKeyPath)
        val token = AuthSecret.loadOrCreate(args.authKeyPath)

        return ServerBuilder.forPort(args.grpcPort)
            .intercept(AuthInterceptor(token))
            .addService(TicketService(ticketStore, matchRepository, queueTypeRepository, eventPublisher))
            .addService(QueueDataService(ticketStore, ticketPool, matchRepository, queueTypeRepository))
            .build()
    }
}

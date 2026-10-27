package net.mythicisland.queue.runtime

import app.simplecloud.api.CloudApi
import app.simplecloud.api.CloudApiOptions
import io.grpc.Server
import io.grpc.ServerBuilder
import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.*
import net.mythicisland.queue.runtime.data.QueueDataService
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.launcher.QueueStartCommand
import net.mythicisland.queue.runtime.match.MatchReconciler
import net.mythicisland.queue.runtime.match.MatchStore
import net.mythicisland.queue.runtime.match.Matchmaker
import net.mythicisland.queue.runtime.transfer.PlayerTransferService
import net.mythicisland.queue.runtime.server.GameServerService
import net.mythicisland.queue.runtime.server.ServerAllocator
import net.mythicisland.queue.runtime.listener.ServerListener
import net.mythicisland.queue.runtime.server.ServerRegistry
import net.mythicisland.queue.runtime.listener.PlayerListener
import net.mythicisland.queue.runtime.state.NatsStateStore
import net.mythicisland.queue.runtime.ticket.TicketService
import net.mythicisland.queue.runtime.ticket.TicketStore
import net.mythicisland.queue.runtime.type.QueueTypeRegistry
import net.mythicisland.queue.shared.auth.AuthInterceptor
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

        val types = QueueTypeRegistry(args.typesPath)
        val nc = connectToNats()
        val api = connectToController()

        val events = EventPublisher(nc)
        val ticketStore = TicketStore(NatsStateStore.init(nc, args.ticketsBucket))
        val matchStore = MatchStore(NatsStateStore.init(nc, args.matchesBucket))
        val registry = ServerRegistry(api, types)

        val allocator = ServerAllocator(api, types, registry, matchStore)
        val reconciler = MatchReconciler(
            types,
            ticketStore,
            matchStore,
            registry,
            allocator,
            PlayerTransferService(api),
            events,
        )
        val ticketService = TicketService(types, ticketStore, reconciler, events)

        val serverListener = ServerListener(api, registry, reconciler)
        val playerListener = PlayerListener(api, ticketStore, ticketService)
        val matchmaker = Matchmaker(types, ticketStore, matchStore, events)

        val server = createGrpcServer(
            ticketService,
            GameServerService(matchStore, allocator, reconciler),
            QueueDataService(types, ticketStore, matchStore),
        )

        try {
            ticketStore.load()
            matchStore.load()
            logger.info("Loaded state", Field("tickets", ticketStore.all().size), Field("matches", matchStore.all().size))

            serverListener.start(types.groups())
            reconciler.recover()
            ticketService.recover()
            allocator.recover()
            playerListener.start()
            matchmaker.start()
            reconciler.start()

            server.start()
        } catch (e: Exception) {
            logger.error("Failed to start queue runtime", Field.error(e))
            shutdown(server, api, nc, serverListener, playerListener, matchmaker, reconciler)
            throw e
        }
        logger.info("gRPC started", Field("port", args.grpcPort))

        val stopped = CountDownLatch(1)
        suspendCancellableCoroutine { continuation ->
            Runtime.getRuntime().addShutdownHook(shutdownHook(continuation, stopped))
        }

        shutdown(server, api, nc, serverListener, playerListener, matchmaker, reconciler)
        stopped.countDown()
    }

    private fun shutdownHook(continuation: CancellableContinuation<Unit>, stopped: CountDownLatch): Thread = Thread {
        continuation.resume(Unit)
        stopped.await(30L, TimeUnit.SECONDS)
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

    private fun createGrpcServer(
        ticketService: TicketService,
        gameServerService: GameServerService,
        queueDataService: QueueDataService,
    ): Server {
        return ServerBuilder.forPort(args.grpcPort)
            .intercept(AuthInterceptor(args.authSecret))
            .addService(ticketService)
            .addService(gameServerService)
            .addService(queueDataService)
            .build()
    }

    private suspend fun shutdown(
        server: Server,
        api: CloudApi,
        nats: Connection,
        serverListener: ServerListener,
        playerListener: PlayerListener,
        matchmaker: Matchmaker,
        reconciler: MatchReconciler,
    ) {
        logger.info("Shutting down queue runtime")

        server.shutdown()
        if (!server.awaitTermination(10L, TimeUnit.SECONDS)) {
            logger.warn("gRPC server did not stop in time, forcing shutdown", Field("timeoutSeconds", 10L))
            server.shutdownNow()
        }

        serverListener.shutdown()
        playerListener.shutdown()
        matchmaker.shutdown()
        reconciler.shutdown()
        api.close()
        nats.close()

        logger.info("Queue runtime stopped")
        LogManager.shutdown()
    }

}

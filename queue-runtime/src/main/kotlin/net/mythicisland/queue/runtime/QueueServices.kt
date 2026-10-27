package net.mythicisland.queue.runtime

import app.simplecloud.api.CloudApi
import io.grpc.Server
import io.grpc.ServerBuilder
import io.nats.client.Connection
import net.mythicisland.queue.runtime.data.QueueDataService
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.launcher.QueueStartCommand
import net.mythicisland.queue.runtime.listener.PlayerListener
import net.mythicisland.queue.runtime.listener.ServerListener
import net.mythicisland.queue.runtime.match.MatchReconciler
import net.mythicisland.queue.runtime.match.MatchStore
import net.mythicisland.queue.runtime.match.Matchmaker
import net.mythicisland.queue.runtime.server.GameServerService
import net.mythicisland.queue.runtime.server.ServerAllocator
import net.mythicisland.queue.runtime.server.ServerRegistry
import net.mythicisland.queue.runtime.state.NatsStateStore
import net.mythicisland.queue.runtime.ticket.TicketService
import net.mythicisland.queue.runtime.ticket.TicketStore
import net.mythicisland.queue.runtime.transfer.PlayerTransferService
import net.mythicisland.queue.runtime.type.QueueTypeRegistry
import net.mythicisland.queue.runtime.type.QueueTypeWatcher
import net.mythicisland.queue.shared.auth.AuthInterceptor
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import java.util.concurrent.TimeUnit

class QueueServices(private val args: QueueStartCommand, nats: Connection, api: CloudApi) {

    private val logger = LoggerFactory.getLogger(QueueServices::class)

    private val types = QueueTypeRegistry(args.typesPath)
    private val events = EventPublisher(nats)
    private val ticketStore = TicketStore(NatsStateStore.init(nats, args.ticketsBucket))
    private val matchStore = MatchStore(NatsStateStore.init(nats, args.matchesBucket))
    private val registry = ServerRegistry(api, types)

    private val allocator = ServerAllocator(api, types, registry, matchStore)
    private val reconciler = MatchReconciler(
        types,
        ticketStore,
        matchStore,
        registry,
        allocator,
        PlayerTransferService(api),
        events,
    )
    private val ticketService = TicketService(types, ticketStore, reconciler, events)

    private val serverListener = ServerListener(api, registry, reconciler)
    private val playerListener = PlayerListener(api, ticketStore, ticketService)
    private val matchmaker = Matchmaker(types, ticketStore, matchStore, events)
    private val typeWatcher = QueueTypeWatcher(types, registry, ticketService)

    private val server: Server = ServerBuilder.forPort(args.grpcPort)
        .intercept(AuthInterceptor(args.authSecret))
        .addService(ticketService)
        .addService(GameServerService(matchStore, allocator, reconciler))
        .addService(QueueDataService(types, ticketStore, matchStore))
        .build()

    suspend fun start() {
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
        typeWatcher.start()

        server.start()
        logger.info("gRPC started", Field("port", args.grpcPort))
    }

    suspend fun shutdown() {
        stopGrpc()
        typeWatcher.shutdown()
        serverListener.shutdown()
        playerListener.shutdown()
        matchmaker.shutdown()
        reconciler.shutdown()
    }

    private fun stopGrpc() {
        server.shutdown()
        if (!server.awaitTermination(10L, TimeUnit.SECONDS)) {
            logger.warn("gRPC server did not stop in time, forcing shutdown", Field("timeoutSeconds", 10L))
            server.shutdownNow()
        }
    }

}

package net.mythicisland.queue.runtime

import app.simplecloud.api.CloudApi
import io.grpc.BindableService
import io.nats.client.Connection
import net.mythicisland.queue.runtime.data.QueueDataService
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.launcher.QueueStartCommand
import net.mythicisland.queue.runtime.listener.PlayerListener
import net.mythicisland.queue.runtime.listener.ServerListener
import net.mythicisland.queue.runtime.match.MatchReconciler
import net.mythicisland.queue.runtime.match.MatchStore
import net.mythicisland.queue.runtime.match.Matchmaker
import net.mythicisland.queue.runtime.metrics.QueueGauges
import net.mythicisland.queue.runtime.metrics.QueueMetrics
import net.mythicisland.queue.runtime.server.GameServerService
import net.mythicisland.queue.runtime.server.ServerAllocator
import net.mythicisland.queue.runtime.server.ServerRegistry
import net.mythicisland.queue.runtime.state.NatsStateStore
import net.mythicisland.queue.runtime.ticket.TicketExpirer
import net.mythicisland.queue.runtime.ticket.TicketService
import net.mythicisland.queue.runtime.ticket.TicketStore
import net.mythicisland.queue.runtime.transfer.PlayerTransferService
import net.mythicisland.queue.runtime.type.QueueTypeManager
import net.mythicisland.queue.runtime.type.QueueTypeRegistry
import net.mythicisland.queue.runtime.type.QueueTypeService
import net.mythicisland.queue.runtime.type.QueueTypeStore
import net.mythicisland.queue.runtime.type.QueueTypeWatcher
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import java.nio.file.Path

class QueueServices(
    private val args: QueueStartCommand,
    nats: Connection,
    api: CloudApi,
    metrics: QueueMetrics,
    gauges: QueueGauges,
) {

    private val logger = LoggerFactory.getLogger(QueueServices::class)

    private val types = QueueTypeRegistry()
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
        metrics,
    )
    private val ticketService = TicketService(types, ticketStore, reconciler, events, metrics)
    private val typeManager = QueueTypeManager(types, QueueTypeStore.init(nats, args.typesBucket), registry, ticketService, events)
    private val typesPath: Path? = args.typesPath
    private val typeWatcher = if (typesPath == null) null else QueueTypeWatcher(typesPath, typeManager)

    private val serverListener = ServerListener(api, registry, reconciler)
    private val playerListener = PlayerListener(api, ticketStore, ticketService)
    private val matchmaker = Matchmaker(types, ticketStore, matchStore, events, metrics)
    private val ticketExpirer = TicketExpirer(types, ticketStore, ticketService)
    private val observation = gauges.observe(types, ticketStore, matchStore, registry)

    val grpcServices: List<BindableService> = listOf(
        ticketService,
        GameServerService(matchStore, allocator, reconciler),
        QueueDataService(types, ticketStore, matchStore),
        QueueTypeService(typeManager, typeWatcher != null),
    )

    suspend fun start() {
        ticketStore.load()
        matchStore.load()
        typeManager.load()
        typeWatcher?.sync()
        logger.info(
            "Loaded state",
            Field("tickets", ticketStore.all().size),
            Field("matches", matchStore.all().size),
            Field("queueTypes", types.all().size),
        )

        serverListener.start(types.groups())
        reconciler.recover()
        ticketService.recover()
        allocator.recover()
        playerListener.start()
        matchmaker.start()
        ticketExpirer.start()
        reconciler.start()
        typeWatcher?.start()
    }

    suspend fun shutdown() {
        typeWatcher?.shutdown()
        serverListener.shutdown()
        playerListener.shutdown()
        matchmaker.shutdown()
        ticketExpirer.shutdown()
        reconciler.shutdown()
        observation.close()
    }

}

package net.mythicisland.queue.runtime

import app.simplecloud.api.CloudApi
import io.grpc.BindableService
import net.mythicisland.queue.runtime.data.QueueDataService
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.launcher.QueueStartCommand
import net.mythicisland.queue.runtime.leader.LeaderElection
import net.mythicisland.queue.runtime.listener.PlayerListener
import net.mythicisland.queue.runtime.listener.ServerListener
import net.mythicisland.queue.runtime.match.MatchReconciler
import net.mythicisland.queue.runtime.match.MatchStore
import net.mythicisland.queue.runtime.match.Matchmaker
import net.mythicisland.queue.runtime.metrics.QueueGauges
import net.mythicisland.queue.runtime.metrics.QueueMetrics
import net.mythicisland.queue.runtime.nats.NatsClient
import net.mythicisland.queue.runtime.server.GameServerService
import net.mythicisland.queue.runtime.server.ServerAllocator
import net.mythicisland.queue.runtime.server.ServerRegistry
import net.mythicisland.queue.runtime.state.NatsStore
import net.mythicisland.queue.runtime.ticket.TicketExpirer
import net.mythicisland.queue.runtime.ticket.TicketService
import net.mythicisland.queue.runtime.ticket.TicketStore
import net.mythicisland.queue.runtime.transfer.PlayerTransferService
import net.mythicisland.queue.runtime.type.QueueTypeManager
import net.mythicisland.queue.runtime.type.QueueTypeRegistry
import net.mythicisland.queue.runtime.type.QueueTypeService
import net.mythicisland.queue.runtime.type.QueueTypeStore
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory

class QueueServices(
    args: QueueStartCommand,
    nats: NatsClient,
    api: CloudApi,
    metrics: QueueMetrics,
    gauges: QueueGauges,
    election: LeaderElection,
    term: Long,
) {

    private val logger = LoggerFactory.getLogger(QueueServices::class)

    private val types = QueueTypeRegistry()
    private val events = EventPublisher(nats.connection)
    private val ticketStore = TicketStore(NatsStore.init(nats, args.ticketsBucket, election, term))
    private val matchStore = MatchStore(NatsStore.init(nats, args.matchesBucket, election, term))
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

    private val serverListener = ServerListener(api, registry, matchStore, reconciler)
    private val playerListener = PlayerListener(api, ticketStore, ticketService)
    private val matchmaker = Matchmaker(types, ticketStore, matchStore, events, metrics)
    private val ticketExpirer = TicketExpirer(types, ticketStore, ticketService)
    private val observation = gauges.observe(types, ticketStore, matchStore, registry)

    val grpcServices: List<BindableService> = listOf(
        ticketService,
        GameServerService(matchStore, allocator, reconciler),
        QueueDataService(types, ticketStore, matchStore),
        QueueTypeService(typeManager),
    )

    suspend fun start() {
        ticketStore.load()
        matchStore.load()
        typeManager.load()
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
    }

    suspend fun shutdown() {
        serverListener.shutdown()
        playerListener.shutdown()
        matchmaker.shutdown()
        ticketExpirer.shutdown()
        reconciler.shutdown()
        observation.close()
    }

}

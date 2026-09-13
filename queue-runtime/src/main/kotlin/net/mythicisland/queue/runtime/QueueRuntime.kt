package net.mythicisland.queue.runtime

import build.buf.gen.mythicisland.queue.v2.MatchState
import io.grpc.Server
import io.grpc.ServerBuilder
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import net.mythicisland.common.Connector
import net.mythicisland.common.auth.AuthInterceptor
import net.mythicisland.common.auth.AuthSecret
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

    private val manager = Connector.connectToNats(args.natsUrl, args.natsUser, args.natsSecret)

    private val publisher = EventPublisher(manager.connection())
    private val queueTypeRepository = QueueTypeRepository(args.typesPath)
    private val matchRepository = MatchRepository()
    private val store = TicketStore()
    private val pool = TicketPool(store)

    private val spanExporter = OtlpGrpcSpanExporter.builder().setEndpoint(args.otlpEndpoint).build()
    private val traceProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
        .setResource(Resource.getDefault().toBuilder().put(ServiceAttributes.SERVICE_NAME, args.serviceName).build())
        .build()
    private val metricExporter = OtlpGrpcMetricExporter.builder().setEndpoint(args.otlpEndpoint).build()
    private val meterProvider = SdkMeterProvider.builder()
        .registerMetricReader(PeriodicMetricReader.builder(metricExporter).build())
        .setResource(Resource.getDefault().toBuilder().put(ServiceAttributes.SERVICE_NAME, args.serviceName).build())
        .build()
    private val sdk = OpenTelemetrySdk.builder()
        .setTracerProvider(traceProvider)
        .setMeterProvider(meterProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .buildAndRegisterGlobal()
    private val tracer: Tracer = sdk.getTracer(args.serviceName)
    private val meter: Meter = sdk.getMeter(args.serviceName)

    suspend fun start() {
        logger.info("Starting Queue...")

        logger.info("Loading queue types...")
        val types = queueTypeRepository.load()
        logger.info("Loaded {} queue types: {}", types.size, types.map { it.name })

        logger.info("Connecting to controller...")
        val api = Connector.connectToController(args.networkId, args.networkSecret, args.controllerUrl, args.controllerNatsUrl)
        val allocator = ServerAllocator(api)

        val matchmaker = Matchmaker(store, pool, matchRepository, queueTypeRepository, publisher)
        val reconciler = MatchReconciler(store, matchRepository, queueTypeRepository, allocator, api, publisher)

        matchmaker.start()
        reconciler.start()

        val server = createGrpcServer()
        startGrpcServer(server)

        suspendCancellableCoroutine { continuation ->
            Runtime.getRuntime().addShutdownHook(Thread {
                logger.info("Shutting down Queue...")
                runBlocking {
                    matchRepository.getAllMatches()
                        .filter { it.state != MatchState.MATCH_STATE_COMPLETED }
                        .forEach { allocator.release(it) }

                    matchmaker.shutdown()
                    reconciler.shutdown()
                    manager.shutdown()
                }
                server.shutdown()
                traceProvider.shutdown()
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
            .addService(TicketService(store, matchRepository, queueTypeRepository, publisher))
            .addService(QueueDataService(store, pool, matchRepository, queueTypeRepository))
            .build()
    }
}

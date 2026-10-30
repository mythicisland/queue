package net.mythicisland.queue.runtime

import app.simplecloud.api.CloudApi
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.protobuf.services.HealthStatusManager
import io.nats.client.Connection
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry
import kotlinx.coroutines.*
import net.mythicisland.queue.runtime.launcher.QueueStartCommand
import net.mythicisland.queue.runtime.metrics.QueueGauges
import net.mythicisland.queue.runtime.metrics.QueueMetrics
import net.mythicisland.queue.runtime.leader.LeaderElection
import net.mythicisland.queue.runtime.leader.LeaderForwarder
import net.mythicisland.queue.runtime.leader.Lease
import net.mythicisland.queue.runtime.leader.QueueRegistry
import net.mythicisland.queue.shared.auth.AuthInterceptor
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds

class QueueNode(
    private val args: QueueStartCommand,
    private val nats: Connection,
    private val api: CloudApi,
    openTelemetry: OpenTelemetry,
) {

    private val logger = LoggerFactory.getLogger(QueueNode::class)
    private val meter = openTelemetry.getMeter("net.mythicisland.queue")
    private val metrics = QueueMetrics(meter)
    private val gauges = QueueGauges(meter)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val self = Lease(args.instanceId, args.advertiseAddress)
    private val election = LeaderElection.init(nats, args.leaderBucket, self, args.leaderTtl.seconds)
    private val forwarder = LeaderForwarder(election.leader, self, args.authSecret)
    private val registry = QueueRegistry(AuthInterceptor(args.authSecret), forwarder.services())
    private val health = HealthStatusManager()
    private val services = AtomicReference<QueueServices?>(null)

    private val server: Server = ServerBuilder.forPort(args.grpcPort)
        .addService(health.healthService)
        .fallbackHandlerRegistry(registry)
        .intercept(GrpcTelemetry.create(openTelemetry).createServerInterceptor())
        .build()

    fun start() {
        health.setStatus("ready", ServingStatus.NOT_SERVING)
        server.start()
        logger.info("gRPC started", Field("port", args.grpcPort), Field("instanceId", self.holder))

        scope.launch {
            election.terms.collect { term ->
                follow()
                if (term != null) {
                    lead()
                }
            }
        }
        scope.launch {
            election.leader.collect { leader ->
                health.setStatus("ready", if (leader == null) ServingStatus.NOT_SERVING else ServingStatus.SERVING)
            }
        }
        election.start()
    }

    suspend fun shutdown() {
        health.enterTerminalState()
        shutdownServer()
        election.stop()
        scope.coroutineContext.job.cancelAndJoin()
        follow()
        election.release()
        forwarder.shutdown()
    }

    private suspend fun lead() {
        try {
            val started = startServices()
            services.set(started)
            registry.lead(started.grpcServices)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to start queue services", Field.error(e))
            election.release()
        }
    }

    private suspend fun startServices(): QueueServices {
        val created = QueueServices(args, nats, api, metrics, gauges)
        try {
            created.start()
            return created
        } catch (e: Exception) {
            created.shutdown()
            throw e
        }
    }

    private suspend fun follow() {
        registry.follow()
        val timeout = 5.seconds
        val stopping = services.getAndSet(null) ?: return
        if (withTimeoutOrNull(timeout) { stopping.shutdown() } == null) {
            logger.error("Halting because queue services did not stop in time", Field("timeoutSeconds", timeout.inWholeSeconds))
            Runtime.getRuntime().halt(1)
        }
    }

    private fun shutdownServer() {
        val timeout = 5.seconds
        server.shutdown()
        if (!server.awaitTermination(timeout.inWholeSeconds, TimeUnit.SECONDS)) {
            logger.warn("gRPC server did not stop in time, forcing shutdown", Field("timeoutSeconds", timeout.inWholeSeconds))
            server.shutdownNow()
        }
    }

}

package net.mythicisland.queue.runtime

import app.simplecloud.api.CloudApi
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.protobuf.services.HealthStatusManager
import io.nats.client.Connection
import kotlinx.coroutines.*
import net.mythicisland.queue.runtime.launcher.QueueStartCommand
import net.mythicisland.queue.runtime.leader.LeaderElection
import net.mythicisland.queue.runtime.leader.LeaderForwarder
import net.mythicisland.queue.runtime.leader.Lease
import net.mythicisland.queue.runtime.leader.RequestRouter
import net.mythicisland.queue.shared.auth.AuthInterceptor
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds

class QueueNode(private val args: QueueStartCommand, private val nats: Connection, private val api: CloudApi) {

    private val logger = LoggerFactory.getLogger(QueueNode::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val self = Lease(args.instanceId, args.advertiseAddress)
    private val election = LeaderElection.init(nats, args.leaderBucket, self, args.leaderTtl.seconds)
    private val forwarder = LeaderForwarder(election.leader, self, args.authSecret)
    private val router = RequestRouter(AuthInterceptor(args.authSecret), forwarder.services())
    private val health = HealthStatusManager()
    private val services = AtomicReference<QueueServices?>(null)

    private val server: Server = ServerBuilder.forPort(args.grpcPort)
        .addService(health.healthService)
        .fallbackHandlerRegistry(router)
        .build()

    fun start() {
        health.setStatus(READINESS, ServingStatus.NOT_SERVING)
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
                health.setStatus(READINESS, if (leader == null) ServingStatus.NOT_SERVING else ServingStatus.SERVING)
            }
        }
        election.start()
    }

    suspend fun shutdown() {
        health.enterTerminalState()
        stopGrpc()
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
            router.lead(started.grpcServices)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to start queue services", Field.error(e))
            election.release()
        }
    }

    private suspend fun startServices(): QueueServices {
        val created = QueueServices(args, nats, api)
        try {
            created.start()
            return created
        } catch (e: Exception) {
            created.shutdown()
            throw e
        }
    }

    private suspend fun follow() {
        router.follow()
        val stopping = services.getAndSet(null) ?: return
        if (withTimeoutOrNull(SHUTDOWN_TIMEOUT) { stopping.shutdown() } == null) {
            logger.error("Halting because queue services did not stop in time", Field("timeoutSeconds", SHUTDOWN_TIMEOUT.inWholeSeconds))
            Runtime.getRuntime().halt(1)
        }
    }

    private fun stopGrpc() {
        server.shutdown()
        if (!server.awaitTermination(SHUTDOWN_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS)) {
            logger.warn("gRPC server did not stop in time, forcing shutdown", Field("timeoutSeconds", SHUTDOWN_TIMEOUT.inWholeSeconds))
            server.shutdownNow()
        }
    }

    private companion object {
        const val READINESS = "ready"
        val SHUTDOWN_TIMEOUT = 5.seconds
    }

}

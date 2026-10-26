package net.mythicisland.queue.runtime

import app.simplecloud.api.CloudApi
import app.simplecloud.api.CloudApiOptions
import io.grpc.Server
import io.grpc.ServerBuilder
import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.*
import net.mythicisland.queue.runtime.launcher.QueueStartCommand
import net.mythicisland.queue.shared.auth.AuthInterceptor
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class QueueRuntime(private val args: QueueStartCommand) {

    private val logger = LoggerFactory.getLogger(QueueRuntime::class)

    suspend fun start() {
        logger.info("Starting queue runtime")

        val nc = connectToNats()
        val api = connectToController()
        val server = createGrpcServer()

        server.start()
        logger.info("gRPC server listening", Field("port", args.grpcPort))

        suspendCancellableCoroutine { continuation ->
            Runtime.getRuntime().addShutdownHook(Thread {
                shutdown(server, api, nc)
                continuation.resume(Unit)
            })
        }
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

    private fun createGrpcServer(): Server {
        return ServerBuilder.forPort(args.grpcPort)
            .intercept(AuthInterceptor(args.authSecret))
            .build()
    }

    private fun shutdown(server: Server, api: CloudApi, nats: Connection) {
        logger.info("Shutting down queue runtime")

        server.shutdown()
        if (!server.awaitTermination(10L, TimeUnit.SECONDS)) {
            logger.warn("gRPC server did not stop in time, forcing shutdown", Field("timeoutSeconds", 10L))
            server.shutdownNow()
        }

        api.close()
        nats.close()

        logger.info("Queue runtime stopped")
    }

}

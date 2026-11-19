package net.mythicisland.queue.api.internal

import build.buf.gen.mythicisland.queue.v1.GameServerServiceGrpcKt.GameServerServiceCoroutineStub
import build.buf.gen.mythicisland.queue.v1.QueueDataServiceGrpcKt.QueueDataServiceCoroutineStub
import build.buf.gen.mythicisland.queue.v1.QueueTypeServiceGrpcKt.QueueTypeServiceCoroutineStub
import build.buf.gen.mythicisland.queue.v1.TicketServiceGrpcKt.TicketServiceCoroutineStub
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import net.mythicisland.queue.api.QueueApi
import net.mythicisland.queue.api.QueueApiOptions
import net.mythicisland.queue.api.data.QueueDataApi
import net.mythicisland.queue.api.event.EventApi
import net.mythicisland.queue.api.internal.data.QueueDataApiImpl
import net.mythicisland.queue.api.internal.event.EventApiImpl
import net.mythicisland.queue.api.internal.match.MatchApiImpl
import net.mythicisland.queue.api.internal.ticket.TicketApiImpl
import net.mythicisland.queue.api.internal.type.QueueTypeApiImpl
import net.mythicisland.queue.api.match.MatchApi
import net.mythicisland.queue.api.ticket.TicketApi
import net.mythicisland.queue.api.type.QueueTypeApi
import java.util.concurrent.TimeUnit

internal class QueueApiImpl(options: QueueApiOptions) : QueueApi {

    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress(options.grpcHost, options.grpcPort)
        .usePlaintext()
        .keepAliveTime(30, TimeUnit.SECONDS)
        .keepAliveTimeout(10, TimeUnit.SECONDS)
        .defaultServiceConfig(SERVICE_CONFIG)
        .enableRetry()
        .build()

    private val connection: Connection = connectNats(options)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val ticketApi: TicketApi
    private val matchApi: MatchApi
    private val dataApi: QueueDataApi
    private val queueTypeApi: QueueTypeApi
    private val eventApi: EventApi

    init {
        val credentials = AuthCredentials(options.authSecret)

        eventApi = EventApiImpl(connection)
        ticketApi = TicketApiImpl(TicketServiceCoroutineStub(channel).withCallCredentials(credentials), scope)
        matchApi = MatchApiImpl(
            GameServerServiceCoroutineStub(channel).withCallCredentials(credentials),
            scope,
            eventApi,
            options.serverId,
        )
        dataApi = QueueDataApiImpl(QueueDataServiceCoroutineStub(channel).withCallCredentials(credentials), scope)
        queueTypeApi = QueueTypeApiImpl(QueueTypeServiceCoroutineStub(channel).withCallCredentials(credentials), scope)
    }

    override fun ticket(): TicketApi = ticketApi

    override fun match(): MatchApi = matchApi

    override fun data(): QueueDataApi = dataApi

    override fun queueType(): QueueTypeApi = queueTypeApi

    override fun event(): EventApi = eventApi

    override fun close() {
        scope.cancel()
        try {
            connection.close()
        } finally {
            shutdownChannel()
        }
    }

    private fun connectNats(options: QueueApiOptions): Connection {
        val natsOptions = Options.builder()
            .server(options.natsUrl)
            .userInfo(options.natsUser, options.natsSecret)
            .maxReconnects(-1)
            .build()

        try {
            return Nats.connect(natsOptions)
        } catch (e: Exception) {
            channel.shutdownNow()
            throw e
        }
    }

    private fun shutdownChannel() {
        channel.shutdown()
        try {
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow()
            }
        } catch (_: InterruptedException) {
            channel.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    private companion object {
        val SERVICE_CONFIG = mapOf(
            "methodConfig" to listOf(
                mapOf(
                    "name" to listOf(
                        mapOf("service" to "mythicisland.queue.v1.TicketService"),
                        mapOf("service" to "mythicisland.queue.v1.GameServerService"),
                        mapOf("service" to "mythicisland.queue.v1.QueueDataService"),
                        mapOf("service" to "mythicisland.queue.v1.QueueTypeService"),
                    ),
                    "retryPolicy" to mapOf(
                        "maxAttempts" to 5.0,
                        "initialBackoff" to "0.5s",
                        "maxBackoff" to "5s",
                        "backoffMultiplier" to 2.0,
                        "retryableStatusCodes" to listOf("UNAVAILABLE"),
                    ),
                ),
            ),
        )
    }

}

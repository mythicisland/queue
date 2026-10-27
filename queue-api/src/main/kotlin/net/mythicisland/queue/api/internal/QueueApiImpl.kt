package net.mythicisland.queue.api.internal

import build.buf.gen.mythicisland.queue.v1.GameServerServiceGrpcKt.GameServerServiceCoroutineStub
import build.buf.gen.mythicisland.queue.v1.QueueDataServiceGrpcKt.QueueDataServiceCoroutineStub
import build.buf.gen.mythicisland.queue.v1.TicketServiceGrpcKt.TicketServiceCoroutineStub
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import net.mythicisland.queue.api.QueueApi
import net.mythicisland.queue.api.QueueApiOptions
import net.mythicisland.queue.api.data.QueueDataApi
import net.mythicisland.queue.api.event.EventApi
import net.mythicisland.queue.api.internal.data.QueueDataApiImpl
import net.mythicisland.queue.api.internal.event.EventApiImpl
import net.mythicisland.queue.api.internal.match.MatchApiImpl
import net.mythicisland.queue.api.internal.ticket.TicketApiImpl
import net.mythicisland.queue.api.match.MatchApi
import net.mythicisland.queue.api.ticket.TicketApi
import java.util.concurrent.TimeUnit

internal class QueueApiImpl(options: QueueApiOptions) : QueueApi {

    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress(options.grpcHost, options.grpcPort)
        .usePlaintext()
        .keepAliveTime(30, TimeUnit.SECONDS)
        .keepAliveTimeout(10, TimeUnit.SECONDS)
        .build()

    private val connection: Connection = connectNats(options)

    private val ticketApi: TicketApi
    private val matchApi: MatchApi
    private val dataApi: QueueDataApi
    private val eventApi: EventApi

    init {
        val credentials = AuthCredentials(options.authSecret)

        ticketApi = TicketApiImpl(TicketServiceCoroutineStub(channel).withCallCredentials(credentials))
        matchApi = MatchApiImpl(GameServerServiceCoroutineStub(channel).withCallCredentials(credentials))
        dataApi = QueueDataApiImpl(QueueDataServiceCoroutineStub(channel).withCallCredentials(credentials))
        eventApi = EventApiImpl(connection)
    }

    override fun ticket(): TicketApi = ticketApi

    override fun match(): MatchApi = matchApi

    override fun data(): QueueDataApi = dataApi

    override fun event(): EventApi = eventApi

    override fun close() {
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

}

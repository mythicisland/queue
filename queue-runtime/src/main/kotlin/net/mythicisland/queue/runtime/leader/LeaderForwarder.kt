package net.mythicisland.queue.runtime.leader

import build.buf.gen.mythicisland.queue.v1.GameServerServiceGrpcKt
import build.buf.gen.mythicisland.queue.v1.QueueDataServiceGrpcKt
import build.buf.gen.mythicisland.queue.v1.TicketServiceGrpcKt
import io.grpc.CallOptions
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.ServiceDescriptor
import io.grpc.Status
import io.grpc.stub.ClientCalls
import io.grpc.stub.MetadataUtils
import io.grpc.stub.ServerCalls
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference

class LeaderForwarder(private val leader: StateFlow<Lease?>, private val self: Lease, authSecret: String) {

    private val auth = MetadataUtils.newAttachHeadersInterceptor(authHeaders(authSecret))
    private val target = AtomicReference<Target?>(null)

    fun services(): List<ServerServiceDefinition> = listOf(
        TicketServiceGrpcKt.serviceDescriptor,
        GameServerServiceGrpcKt.serviceDescriptor,
        QueueDataServiceGrpcKt.serviceDescriptor,
    ).map { forwarding(it) }

    fun shutdown() {
        target.getAndSet(null)?.channel?.shutdown()
    }

    private fun forwarding(service: ServiceDescriptor): ServerServiceDefinition {
        val definition = ServerServiceDefinition.builder(service.name)
        for (method in service.methods) {
            val forwarded = method.toBuilder(BytesMarshaller, BytesMarshaller).build()
            definition.addMethod(forwarded, ServerCalls.asyncUnaryCall { request, response ->
                forward(forwarded, request, response)
            })
        }
        return definition.build()
    }

    private fun forward(method: MethodDescriptor<ByteArray, ByteArray>, request: ByteArray, response: StreamObserver<ByteArray>) {
        val channel = channel()
        if (channel == null) {
            response.onError(Status.UNAVAILABLE.withDescription("No queue leader is ready").asException())
            return
        }
        ClientCalls.asyncUnaryCall(channel.newCall(method, CallOptions.DEFAULT), request, response)
    }

    private fun channel(): ManagedChannel? {
        val lease = leader.value
        if (lease == null || lease == self) {
            return null
        }

        val address = lease.address
        val current = target.get()
        if (current != null && current.address == address) {
            return current.channel
        }

        val created = Target(address, ManagedChannelBuilder.forTarget(address).usePlaintext().intercept(auth).build())
        if (!target.compareAndSet(current, created)) {
            created.channel.shutdown()
            return channel()
        }
        current?.channel?.shutdown()
        return created.channel
    }

    private fun authHeaders(secret: String): Metadata {
        val headers = Metadata()
        headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer $secret")
        return headers
    }

    private class Target(val address: String, val channel: ManagedChannel)

    private object BytesMarshaller : MethodDescriptor.Marshaller<ByteArray> {
        override fun stream(value: ByteArray): InputStream = ByteArrayInputStream(value)

        override fun parse(stream: InputStream): ByteArray = stream.readAllBytes()
    }

}

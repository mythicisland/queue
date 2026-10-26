package net.mythicisland.queue.shared.auth

import io.grpc.Grpc
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import java.security.MessageDigest

class AuthInterceptor(secret: String) : ServerInterceptor {

    private val logger = LoggerFactory.getLogger(AuthInterceptor::class)
    private val expected = "Bearer $secret".toByteArray(Charsets.UTF_8)

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val provided = headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER))

        if (provided == null || !isValid(provided)) {
            logger.warn(
                "Rejected unauthenticated call",
                Field("method", call.methodDescriptor.fullMethodName),
                Field("remote", call.attributes.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR)),
            )
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid credentials"), Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }

        return next.startCall(call, headers)
    }

    private fun isValid(provided: String): Boolean =
        MessageDigest.isEqual(provided.toByteArray(Charsets.UTF_8), expected)

}
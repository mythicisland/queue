package net.mythicisland.queue.api.internal

import io.grpc.CallCredentials
import io.grpc.Metadata
import java.util.concurrent.Executor

internal class AuthCredentials(secret: String) : CallCredentials() {

    private val metadata = Metadata()

    init {
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer $secret")
    }

    override fun applyRequestMetadata(requestInfo: RequestInfo, appExecutor: Executor, applier: MetadataApplier) {
        applier.apply(metadata)
    }

}

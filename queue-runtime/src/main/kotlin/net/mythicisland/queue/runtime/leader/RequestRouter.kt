package net.mythicisland.queue.runtime.leader

import io.grpc.BindableService
import io.grpc.HandlerRegistry
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.ServerMethodDefinition
import io.grpc.ServerServiceDefinition
import java.util.concurrent.atomic.AtomicReference

class RequestRouter(private val auth: ServerInterceptor, forwarded: List<ServerServiceDefinition>) : HandlerRegistry() {

    private val forwarding = index(forwarded)
    private val methods = AtomicReference(forwarding)

    fun lead(services: List<BindableService>) {
        methods.set(index(services.map { it.bindService() }))
    }

    fun follow() {
        methods.set(forwarding)
    }

    override fun lookupMethod(methodName: String, authority: String?): ServerMethodDefinition<*, *>? =
        methods.get()[methodName]

    private fun index(services: List<ServerServiceDefinition>): Map<String, ServerMethodDefinition<*, *>> =
        services.map { ServerInterceptors.intercept(it, auth) }
            .flatMap { it.methods }
            .associateBy { it.methodDescriptor.fullMethodName }

}

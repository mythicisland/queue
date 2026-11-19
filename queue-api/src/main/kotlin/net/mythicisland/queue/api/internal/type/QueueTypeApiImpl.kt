package net.mythicisland.queue.api.internal.type

import build.buf.gen.mythicisland.queue.v1.CreateQueueTypeRequest
import build.buf.gen.mythicisland.queue.v1.DeleteQueueTypeRequest
import build.buf.gen.mythicisland.queue.v1.QueueTypeServiceGrpcKt.QueueTypeServiceCoroutineStub
import build.buf.gen.mythicisland.queue.v1.SetQueueTypeActiveRequest
import build.buf.gen.mythicisland.queue.v1.UpdateQueueTypeRequest
import io.grpc.StatusException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import net.mythicisland.queue.api.internal.ProtoUtil
import net.mythicisland.queue.api.type.QueueType
import net.mythicisland.queue.api.type.QueueTypeApi
import java.util.concurrent.CompletableFuture

internal class QueueTypeApiImpl(
    private val stub: QueueTypeServiceCoroutineStub,
    private val scope: CoroutineScope,
) : QueueTypeApi {

    override suspend fun createQueueType(type: QueueType, validateOnly: Boolean): QueueType {
        val request = CreateQueueTypeRequest.newBuilder()
            .setQueueType(ProtoUtil.fromQueueType(type))
            .setValidateOnly(validateOnly)
            .build()

        try {
            return ProtoUtil.toQueueType(stub.createQueueType(request).queueType)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override suspend fun updateQueueType(type: QueueType, validateOnly: Boolean): QueueType {
        val request = UpdateQueueTypeRequest.newBuilder()
            .setQueueType(ProtoUtil.fromQueueType(type))
            .setValidateOnly(validateOnly)
            .build()

        try {
            return ProtoUtil.toQueueType(stub.updateQueueType(request).queueType)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override suspend fun deleteQueueType(name: String) {
        val request = DeleteQueueTypeRequest.newBuilder()
            .setName(name)
            .build()

        try {
            stub.deleteQueueType(request)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override suspend fun setQueueTypeActive(name: String, active: Boolean): QueueType {
        val request = SetQueueTypeActiveRequest.newBuilder()
            .setName(name)
            .setActive(active)
            .build()

        try {
            return ProtoUtil.toQueueType(stub.setQueueTypeActive(request).queueType)
        } catch (e: StatusException) {
            throw ProtoUtil.toQueueException(e)
        }
    }

    override fun createQueueTypeAsync(type: QueueType, validateOnly: Boolean): CompletableFuture<QueueType> =
        scope.future { createQueueType(type, validateOnly) }

    override fun updateQueueTypeAsync(type: QueueType, validateOnly: Boolean): CompletableFuture<QueueType> =
        scope.future { updateQueueType(type, validateOnly) }

    override fun deleteQueueTypeAsync(name: String): CompletableFuture<Void?> = scope.future {
        deleteQueueType(name)
        null
    }

    override fun setQueueTypeActiveAsync(name: String, active: Boolean): CompletableFuture<QueueType> =
        scope.future { setQueueTypeActive(name, active) }

}

package net.mythicisland.queue.runtime.type

import build.buf.gen.mythicisland.queue.v1.CreateQueueTypeRequest
import build.buf.gen.mythicisland.queue.v1.CreateQueueTypeResponse
import build.buf.gen.mythicisland.queue.v1.DeleteQueueTypeRequest
import build.buf.gen.mythicisland.queue.v1.DeleteQueueTypeResponse
import build.buf.gen.mythicisland.queue.v1.QueueTypeServiceGrpcKt
import build.buf.gen.mythicisland.queue.v1.SetQueueTypeActiveRequest
import build.buf.gen.mythicisland.queue.v1.SetQueueTypeActiveResponse
import build.buf.gen.mythicisland.queue.v1.UpdateQueueTypeRequest
import build.buf.gen.mythicisland.queue.v1.UpdateQueueTypeResponse
import build.buf.gen.mythicisland.queue.v1.createQueueTypeResponse
import build.buf.gen.mythicisland.queue.v1.setQueueTypeActiveResponse
import build.buf.gen.mythicisland.queue.v1.updateQueueTypeResponse
import net.mythicisland.queue.shared.type.QueueType

class QueueTypeService(private val manager: QueueTypeManager) : QueueTypeServiceGrpcKt.QueueTypeServiceCoroutineImplBase() {

    override suspend fun createQueueType(request: CreateQueueTypeRequest): CreateQueueTypeResponse {
        val created = manager.create(QueueType.fromProto(request.queueType), request.validateOnly)
        return createQueueTypeResponse { this.queueType = created.toProto() }
    }

    override suspend fun updateQueueType(request: UpdateQueueTypeRequest): UpdateQueueTypeResponse {
        val updated = manager.update(QueueType.fromProto(request.queueType), request.validateOnly)
        return updateQueueTypeResponse { this.queueType = updated.toProto() }
    }

    override suspend fun deleteQueueType(request: DeleteQueueTypeRequest): DeleteQueueTypeResponse {
        manager.delete(request.name)
        return DeleteQueueTypeResponse.getDefaultInstance()
    }

    override suspend fun setQueueTypeActive(request: SetQueueTypeActiveRequest): SetQueueTypeActiveResponse {
        val updated = manager.setActive(request.name, request.active)
        return setQueueTypeActiveResponse { this.queueType = updated.toProto() }
    }

}

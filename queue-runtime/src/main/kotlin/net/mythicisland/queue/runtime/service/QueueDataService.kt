package net.mythicisland.queue.runtime.service

import build.buf.gen.mythicisland.queue.v1.*
import io.grpc.Status
import net.mythicisland.moonrise.common.extension.asUUID
import net.mythicisland.queue.shared.queue.QueueType
import net.mythicisland.queue.runtime.repository.QueueRepository
import net.mythicisland.queue.runtime.repository.QueueTypeRepository

class QueueDataService(
    private val queues: QueueRepository,
    private val types: QueueTypeRepository,
) : QueueDataServiceGrpcKt.QueueDataServiceCoroutineImplBase() {

    override suspend fun getQueue(request: GetQueueRequest): GetQueueResponse {
        val id = request.queueId.asUUID()
        val queue = queues.getQueue(id)
            ?: throw Status.NOT_FOUND
                .withDescription("Queue '$id' not found")
                .asRuntimeException()

        return getQueueResponse { this.queue = queue.toDefinition() }
    }

    override suspend fun getAllQueues(request: GetAllQueuesRequest): GetAllQueuesResponse {
        val queues = queues.getAllQueues()

        return getAllQueuesResponse {
            this.queues.addAll(queues.map { queue ->
                queue.toDefinition()
            })
        }
    }

    override suspend fun getQueuesByType(request: GetQueuesByTypeRequest): GetQueuesByTypeResponse {
        val queues = queues.getAllQueuesByType(request.type)

        return getQueuesByTypeResponse {
            this.queues.addAll(queues.map { queue ->
                queue.toDefinition()
            })
        }
    }

    override suspend fun getQueueByPlayer(request: GetQueueByPlayerRequest): GetQueueByPlayerResponse {
        val player = request.playerId.asUUID()
        val queue = queues.getQueueByPlayer(player)
            ?: throw Status.NOT_FOUND
                .withDescription("Player '$player' is not in any queue")
                .asRuntimeException()

        return getQueueByPlayerResponse { this.queue = queue.toDefinition() }
    }

    override suspend fun getPlayerPosition(request: GetPlayerPositionRequest): GetPlayerPositionResponse {
        val player = request.playerId.asUUID()
        val queue = queues.getQueueByPlayer(player)
            ?: throw Status.NOT_FOUND
                .withDescription("Player '$player' is not in any queue")
                .asRuntimeException()

        val position = queue.players.indexOf(player) + 1

        return getPlayerPositionResponse {
            this.queue = queue.toDefinition()
            this.position = position
        }
    }

    override suspend fun getQueueType(request: GetQueueTypeRequest): GetQueueTypeResponse {
        val type = types.find(request.name)
            ?: throw Status.NOT_FOUND
                .withDescription("Queue type '${request.name}' not found")
                .asRuntimeException()

        return getQueueTypeResponse { this.queueType = type.toDefinition() }
    }

    override suspend fun getAllQueueTypes(request: GetAllQueueTypesRequest): GetAllQueueTypesResponse {
        val types = types.getAll()

        return getAllQueueTypesResponse {
            this.queueTypes.addAll(types.map(QueueType::toDefinition))
        }
    }
}

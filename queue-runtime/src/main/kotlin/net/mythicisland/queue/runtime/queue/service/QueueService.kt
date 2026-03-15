package net.mythicisland.queue.runtime.queue.service

import build.buf.gen.mythicisland.queue.v1.*
import net.mythicisland.queue.runtime.queue.repository.QueueRepository
import org.apache.logging.log4j.LogManager

class QueueService(
    private val queues: QueueRepository
) : QueueServiceGrpcKt.QueueServiceCoroutineImplBase() {

    private val logger = LogManager.getLogger(QueueService::class.java)

    override suspend fun dequeue(request: DequeueRequest): DequeueResponse {

        return dequeueResponse {  }
    }

    override suspend fun enqueue(request: EnqueueRequest): EnqueueResponse {

        return enqueueResponse {  }
    }

}
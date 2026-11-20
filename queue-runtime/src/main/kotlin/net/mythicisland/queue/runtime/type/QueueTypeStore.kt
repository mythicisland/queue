package net.mythicisland.queue.runtime.type

import io.nats.client.KeyValue
import net.mythicisland.queue.runtime.nats.NatsClient
import net.mythicisland.queue.shared.type.QueueType
import build.buf.gen.mythicisland.queue.v1.QueueType as QueueTypeProto

class QueueTypeStore(private val kv: KeyValue) {

    fun load(): List<QueueType> {
        kv.purgeDeletes()
        return kv.keys()
            .mapNotNull { key -> kv.get(key)?.value }
            .map { bytes -> QueueType.fromProto(QueueTypeProto.parseFrom(bytes)) }
    }

    fun put(type: QueueType) {
        kv.put(type.name, type.toProto().toByteArray())
    }

    fun delete(name: String) {
        kv.delete(name)
    }

    companion object {
        fun init(nats: NatsClient, bucket: String): QueueTypeStore = QueueTypeStore(nats.bucket(bucket))
    }

}

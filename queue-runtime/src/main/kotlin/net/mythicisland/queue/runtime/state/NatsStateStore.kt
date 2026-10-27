package net.mythicisland.queue.runtime.state

import io.nats.client.Connection
import io.nats.client.JetStreamOptions
import io.nats.client.KeyValue
import io.nats.client.KeyValueOptions
import io.nats.client.api.KeyValueConfiguration
import io.nats.client.api.StorageType
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import java.time.Duration

class NatsStateStore(private val bucket: String, private val kv: KeyValue) : StateStore {

    private val logger = LoggerFactory.getLogger(NatsStateStore::class)

    override fun load(): List<ByteArray> {
        kv.purgeDeletes()
        return kv.keys().mapNotNull { key -> kv.get(key)?.value }
    }

    override fun put(key: String, value: ByteArray) {
        try {
            kv.put(key, value)
        } catch (e: Exception) {
            sendFailure("put", key, e)
        }
    }

    override fun delete(key: String) {
        try {
            kv.delete(key)
        } catch (e: Exception) {
            sendFailure("delete", key, e)
        }
    }

    private fun sendFailure(operation: String, key: String, e: Exception) {
        logger.error("Failed to write state", Field("bucket", bucket), Field("operation", operation), Field("key", key), Field.error(e))
    }

    companion object {
        fun init(nats: Connection, bucket: String): NatsStateStore {
            val management = nats.keyValueManagement()
            if (bucket !in management.bucketNames) {
                val config = KeyValueConfiguration.builder()
                    .name(bucket)
                    .storageType(StorageType.File)
                    .build()
                management.create(config)
            }
            val options = KeyValueOptions.builder()
                .jetStreamOptions(JetStreamOptions.builder().requestTimeout(Duration.ofMillis(500)).build())
                .build()
            return NatsStateStore(bucket, nats.keyValue(bucket, options))
        }

    }

}

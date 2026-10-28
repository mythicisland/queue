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
import java.util.concurrent.ConcurrentHashMap

class NatsStateStore(private val bucket: String, private val kv: KeyValue) : StateStore {

    private val logger = LoggerFactory.getLogger(NatsStateStore::class)
    private val failedPuts = ConcurrentHashMap<String, ByteArray>()
    // https://youtrack.jetbrains.com/issue/KT-18053
    private val failedDeletes = ConcurrentHashMap.newKeySet<String>()

    override fun load(): List<ByteArray> {
        kv.purgeDeletes()
        return kv.keys().mapNotNull { key -> kv.get(key)?.value }
    }

    override fun put(key: String, value: ByteArray) {
        val error = tryPut(key, value) ?: return
        sendFailure("put", key, error)
    }

    override fun delete(key: String) {
        val error = tryDelete(key) ?: return
        sendFailure("delete", key, error)
    }

    override fun retryFailed() {
        for ((key, value) in failedPuts) {
            if (tryPut(key, value) != null) {
                return
            }
            logger.info("Retried state write", Field("bucket", bucket), Field("operation", "put"), Field("key", key))
        }
        for (key in failedDeletes) {
            if (tryDelete(key) != null) {
                return
            }
            logger.info("Retried state write", Field("bucket", bucket), Field("operation", "delete"), Field("key", key))
        }
    }

    private fun tryPut(key: String, value: ByteArray): Exception? {
        failedDeletes.remove(key)
        try {
            kv.put(key, value)
            failedPuts.remove(key)
            return null
        } catch (e: Exception) {
            failedPuts[key] = value
            return e
        }
    }

    private fun tryDelete(key: String): Exception? {
        failedPuts.remove(key)
        try {
            kv.delete(key)
            failedDeletes.remove(key)
            return null
        } catch (e: Exception) {
            failedDeletes.add(key)
            return e
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

package net.mythicisland.queue.runtime.state

import io.nats.client.Connection
import io.nats.client.KeyValue
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory

class NatsStateStore(private val bucket: String, private val kv: KeyValue) : StateStore {

    private val logger = LoggerFactory.getLogger(NatsStateStore::class)
    private val failed = mutableMapOf<String, ByteArray?>()

    override fun load(): List<ByteArray> {
        kv.purgeDeletes()
        return kv.keys().mapNotNull { key -> kv.get(key)?.value }
    }

    override fun put(key: String, value: ByteArray) = write(key, value)

    override fun delete(key: String) = write(key, null)

    override fun retryFailed() {
        for ((key, value) in failed.toList()) {
            if (tryWrite(key, value) != null) {
                return
            }
            logger.info("Retried state write", Field("bucket", bucket), Field("key", key))
        }
    }

    private fun write(key: String, value: ByteArray?) {
        val error = tryWrite(key, value) ?: return
        logger.error("Failed to write state", Field("bucket", bucket), Field("key", key), Field.error(error))
    }

    private fun tryWrite(key: String, value: ByteArray?): Exception? {
        try {
            if (value == null) {
                kv.delete(key)
            } else {
                kv.put(key, value)
            }
            failed.remove(key)
            return null
        } catch (e: Exception) {
            failed[key] = value
            return e
        }
    }

    companion object {
        fun init(nc: Connection, bucket: String): NatsStateStore = NatsStateStore(bucket, KeyValueBuckets.open(nc, bucket))
    }

}

package net.mythicisland.queue.runtime.state

import io.nats.client.KeyValue
import net.mythicisland.queue.runtime.nats.NatsClient
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory

class NatsStore(private val bucket: String, private val kv: KeyValue) : StateStore {

    private val logger = LoggerFactory.getLogger(NatsStore::class)
    private val failed = mutableMapOf<String, ByteArray?>()

    override fun load(): List<ByteArray> {
        kv.purgeDeletes()
        return kv.keys().mapNotNull { key -> kv.get(key)?.value }
    }

    override fun put(key: String, value: ByteArray) {
        write(key, value)
    }

    override fun delete(key: String) {
        write(key, null)
    }

    override fun retryFailed() {
        for ((key, value) in failed.toList()) {
            if (!write(key, value)) {
                return
            }
        }
    }

    private fun write(key: String, value: ByteArray?): Boolean {
        try {
            if (value == null) {
                kv.delete(key)
            } else {
                kv.put(key, value)
            }
        } catch (e: Exception) {
            if (failed.isEmpty()) {
                logger.warn("Failed to write state, retrying", Field("bucket", bucket), Field("key", key), Field.error(e))
            }
            failed[key] = value
            return false
        }

        if (key in failed) {
            failed.remove(key)
            if (failed.isEmpty()) {
                logger.info("State writes recovered", Field("bucket", bucket))
            }
        }
        return true
    }

    companion object {
        fun init(nats: NatsClient, bucket: String): NatsStore = NatsStore(bucket, nats.bucket(bucket))
    }

}

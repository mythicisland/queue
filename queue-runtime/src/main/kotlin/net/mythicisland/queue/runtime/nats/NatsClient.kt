package net.mythicisland.queue.runtime.nats

import io.nats.client.Connection
import io.nats.client.JetStreamOptions
import io.nats.client.KeyValue
import io.nats.client.KeyValueOptions
import io.nats.client.Nats
import io.nats.client.Options
import io.nats.client.api.KeyValueConfiguration
import io.nats.client.api.StorageType
import kotlinx.coroutines.*
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import java.io.IOException
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

class NatsClient(val connection: Connection, private val listener: NatsListener) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val buckets = ConcurrentHashMap<String, KeyValueConfiguration>()

    fun start() {
        scope.launch {
            listener.reconnects.collect { restoreBuckets() }
        }
    }

    fun bucket(name: String): KeyValue = bucket(
        KeyValueConfiguration.builder()
            .name(name)
            .storageType(StorageType.File)
            .build()
    )

    fun bucket(config: KeyValueConfiguration): KeyValue {
        ensure(config)
        buckets[config.bucketName] = config
        return connection.keyValue(config.bucketName, bucketOptions)
    }

    suspend fun close() {
        scope.coroutineContext.job.cancelAndJoin()
        connection.close()
    }

    private fun restoreBuckets() {
        for (config in buckets.values) {
            try {
                if (ensure(config)) {
                    logger.warn("Recreated missing NATS bucket", Field("bucket", config.bucketName))
                }
            } catch (e: Exception) {
                logger.error("Failed to restore NATS bucket", Field("bucket", config.bucketName), Field.error(e))
            }
        }
    }

    private fun ensure(config: KeyValueConfiguration): Boolean {
        val management = connection.keyValueManagement(bucketOptions)
        if (config.bucketName in management.bucketNames) {
            management.update(config)
            return false
        }
        management.create(config)
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NatsClient::class)
        private val bucketOptions = KeyValueOptions.builder()
            .jetStreamOptions(JetStreamOptions.builder().requestTimeout(Duration.ofMillis(500)).build())
            .build()

        suspend fun init(url: String, user: String, secret: String): NatsClient {
            val listener = NatsListener()
            val client = NatsClient(connect(url, user, secret, listener), listener)
            client.start()
            return client
        }

        private suspend fun connect(url: String, user: String, secret: String, listener: NatsListener): Connection {
            val options = Options.builder()
                .server(url)
                .userInfo(user, secret)
                .maxReconnects(-1)
                .connectionListener(listener)
                .errorListener(listener)
                .build()

            logger.info("Connecting to NATS", Field("url", url))
            try {
                return Nats.connect(options)
            } catch (e: IOException) {
                logger.warn("NATS is not reachable, retrying until it is", Field("url", url), Field.error(e))
            }
            while (true) {
                delay(2.seconds)
                try {
                    return Nats.connect(options)
                } catch (_: IOException) {
                    continue
                }
            }
        }
    }

}

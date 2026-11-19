package net.mythicisland.queue.runtime.state

import io.nats.client.Connection
import io.nats.client.JetStreamOptions
import io.nats.client.KeyValue
import io.nats.client.KeyValueOptions
import io.nats.client.api.KeyValueConfiguration
import io.nats.client.api.StorageType
import java.time.Duration

object KeyValueBuckets {

    fun open(nats: Connection, bucket: String): KeyValue {
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
        return nats.keyValue(bucket, options)
    }

}

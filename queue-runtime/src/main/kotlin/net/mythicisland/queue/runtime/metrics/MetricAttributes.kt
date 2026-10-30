package net.mythicisland.queue.runtime.metrics

import io.opentelemetry.api.common.AttributeKey

object MetricAttributes {

    val QUEUE_TYPE: AttributeKey<String> = AttributeKey.stringKey("queue.type")
    val SERVER_GROUP: AttributeKey<String> = AttributeKey.stringKey("server.group")
    val STATE: AttributeKey<String> = AttributeKey.stringKey("state")
    val REASON: AttributeKey<String> = AttributeKey.stringKey("reason")
    val RESULT: AttributeKey<String> = AttributeKey.stringKey("result")
    val KIND: AttributeKey<String> = AttributeKey.stringKey("kind")

}

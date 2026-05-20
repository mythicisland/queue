package net.mythicisland.common.nats

import com.google.protobuf.MessageLite
import io.nats.client.Connection
import org.apache.logging.log4j.LogManager

/**
 * Publisher to publish protobuf messages to NATS.
 */
abstract class Publisher(
    private val connection: Connection
) {
    private val logger = LogManager.getLogger(this::class.java)

    /**
     * Publishes a protobuf message to the given NATS subject.
     *
     * @param subject The NATS subject to publish to
     * @param message The protobuf message to serialize and publish
     */
    protected fun publish(subject: String, message: MessageLite) {
        try {
            connection.publish(subject, message.toByteArray())
            logger.debug("Published event to {}", subject)
        } catch (e: Exception) {
            logger.error("Failed to publish event to {}", subject, e)
        }
    }
}
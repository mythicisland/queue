package net.mythicisland.queue.api.builders

import net.mythicisland.queue.api.QueueApi
import net.mythicisland.queue.api.QueueApiOptions
import java.time.Duration

/**
 * Kotlin DSL builder for [QueueApiOptions].
 *
 * Example usage:
 * ```kotlin
 * val options = queueApiOptions {
 *     grpcHost = "localhost"
 *     grpcPort = 4564
 *     natsUrl = "nats://localhost:4222"
 *     natsUser = "user"
 *     natsSecret = "secret"
 *     natsFailoverReconnectAfter = Duration.ofSeconds(30)
 * }
 * ```
 *
 * @see queueApi
 */
class QueueApiOptionsBuilder {

    /** The gRPC server hostname. Defaults to environment variable or "localhost". */
    var grpcHost: String? = null

    /** The gRPC server port (1–65535). Defaults to environment variable or 4564. */
    var grpcPort: Int? = null

    /** The NATS connection URL. */
    var natsUrl: String? = null

    /** The NATS authentication username. */
    var natsUser: String? = null

    /** The NATS authentication secret. */
    var natsSecret: String? = null

    /** Duration before triggering a full NATS reconnect. Defaults to 30 seconds. */
    var natsFailoverReconnectAfter: Duration? = null

    /**
     * Builds the [QueueApiOptions] from the current builder state.
     * Only explicitly set properties override the defaults.
     *
     * @return the constructed options
     */
    fun build(): QueueApiOptions {
        val builder = QueueApiOptions.builder()
        grpcHost?.let { builder.grpcHost(it) }
        grpcPort?.let { builder.grpcPort(it) }
        natsUrl?.let { builder.natsUrl(it) }
        natsUser?.let { builder.natsUser(it) }
        natsSecret?.let { builder.natsSecret(it) }
        natsFailoverReconnectAfter?.let { builder.natsFailoverReconnectAfter(it) }
        return builder.build()
    }
}

/**
 * Creates [QueueApiOptions] using a Kotlin DSL.
 *
 * ```kotlin
 * val options = queueApiOptions {
 *     grpcHost = "localhost"
 *     grpcPort = 4564
 * }
 * ```
 *
 * @param block the configuration block
 * @return the constructed options
 */
fun queueApiOptions(block: QueueApiOptionsBuilder.() -> Unit): QueueApiOptions {
    return QueueApiOptionsBuilder().apply(block).build()
}

/**
 * Creates a [QueueApi] instance using a Kotlin DSL for configuration.
 *
 * ```kotlin
 * val api = queueApi {
 *     grpcHost = "localhost"
 *     grpcPort = 4564
 *     natsUrl = "nats://localhost:4222"
 * }
 * ```
 *
 * @param block the configuration block
 * @return a new [QueueApi] instance
 */
fun queueApi(block: QueueApiOptionsBuilder.() -> Unit): QueueApi {
    return QueueApi.create(queueApiOptions(block))
}

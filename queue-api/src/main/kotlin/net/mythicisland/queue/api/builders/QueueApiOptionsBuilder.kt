package net.mythicisland.queue.api.builders

import net.mythicisland.queue.api.QueueApiOptions
import net.mythicisland.queue.api.QueueDsl

/**
 * Builds [QueueApiOptions] from a Kotlin block.
 *
 * Every property starts out with the value from [QueueApiOptions.DEFAULT], so
 * anything not set explicitly still comes from the environment variables.
 *
 * ```
 * val api = queueApi {
 *     grpcHost = "queue.internal"
 *     token = System.getenv("QUEUE_TOKEN")
 * }
 * ```
 *
 * @param defaults the options to start from.
 */
@QueueDsl
class QueueApiOptionsBuilder internal constructor(
    defaults: QueueApiOptions = QueueApiOptions.DEFAULT,
) {

    /**
     * The host the queue gRPC server runs on.
     */
    var grpcHost: String = defaults.grpcHost()

    /**
     * The port the queue gRPC server listens on.
     */
    var grpcPort: Int = defaults.grpcPort()

    /**
     * The URL of the NATS server the events are read from.
     */
    var natsUrl: String = defaults.natsUrl()

    /**
     * The user for the NATS connection.
     */
    var natsUser: String = defaults.natsUser()

    /**
     * The secret for the NATS connection.
     */
    var natsSecret: String = defaults.natsSecret()

    /**
     * The shared auth token sent with every gRPC call.
     */
    var token: String = defaults.token()

    internal fun build(): QueueApiOptions {
        return QueueApiOptions(grpcHost, grpcPort, natsUrl, natsUser, natsSecret, token)
    }

}

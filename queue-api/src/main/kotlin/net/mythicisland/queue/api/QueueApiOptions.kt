package net.mythicisland.queue.api

/**
 * Connection settings for the [QueueApi].
 *
 * Every value falls back to an environment variable, then to the runtime defaults.
 *
 * @throws IllegalArgumentException if [grpcHost] is blank or [grpcPort] is out of range.
 */
data class QueueApiOptions(
    val grpcHost: String = System.getenv("QUEUE_GRPC_HOST") ?: "localhost",
    val grpcPort: Int = System.getenv("QUEUE_GRPC_PORT")?.toInt() ?: 4564,
    val natsUrl: String = System.getenv("QUEUE_NATS_URL") ?: "nats://localhost:4222",
    val natsUser: String = System.getenv("QUEUE_NATS_USER") ?: "admin",
    val natsSecret: String = System.getenv("QUEUE_NATS_SECRET") ?: "sup3rS3cr3t",
    val authSecret: String = System.getenv("QUEUE_AUTH_SECRET") ?: "sup3rS3cr3t",
    val serverId: String? = System.getenv("SIMPLECLOUD_UNIQUE_ID"),
) {

    init {
        require(grpcHost.isNotBlank()) { "grpcHost must not be blank" }
        require(grpcPort in 1..65535) { "grpcPort must be between 1 and 65535, got: $grpcPort" }
    }

}

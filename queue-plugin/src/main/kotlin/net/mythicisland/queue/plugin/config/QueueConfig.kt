package net.mythicisland.queue.plugin.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import java.time.Duration

@ConfigSerializable
data class QueueConfig(
    @Comment("The internal version of this config, please don't change this.")
    val version: Char = '1',

    @Comment("The Service Configuration from Queue")
    val queue: ServiceConfig = ServiceConfig()
)

@ConfigSerializable
data class ServiceConfig(
    @Comment("The gRPC Host from the Queue Droplet.")
    val grpcHost: String = "localhost",
    @Comment("The gRPC Port from the Queue Droplet.")
    val grpcPort: Int = 4564,
    @Comment("The REST url from the Queue Droplet.")
    val url: String = "https://queue.mythicisland.net/v1/",
    @Comment("The NATS url from the Queue Droplet.")
    val natsUrl: String = "nats://platform.mythicisland.net:4222",
    @Comment("The NATS user to connect with the NATS server.")
    val natsUser: String = "admin",
    @Comment("The NATS secret to connect with the user.")
    val natsSecret: String = "your-super-secret-password",
    @Comment("The internal duration to reconnect with NATS.")
    val natsFailoverReconnectAfter: Duration = Duration.ofSeconds(30)
)
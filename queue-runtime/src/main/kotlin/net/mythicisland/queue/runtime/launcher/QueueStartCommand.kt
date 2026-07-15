package net.mythicisland.queue.runtime.launcher

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.sources.PropertiesValueSource
import com.github.ajalt.clikt.sources.ValueSource
import net.mythicisland.queue.runtime.QueueRuntime
import java.io.File
import java.nio.file.Path

object QueueStartCommand : SuspendingCliktCommand() {

    init {
        context {
            valueSource = PropertiesValueSource.from(File("queue.properties"), false, ValueSource.envvarKey())
        }
    }

    val grpcPort: Int by option(help = "Port for the Queue gRPC server (default: 4564)", envvar = "GRPC_PORT")
        .int().default(4564)

    val natsUrl: String by option(help = "URL from the NATS server used for event publishing (default: nats://localhost:4222)", envvar = "NATS_URL")
        .default("nats://localhost:4222")

    val natsUser: String by option(help = "User from the NATS server (default: admin)", envvar = "NATS_USER")
        .default("admin")

    val natsSecret: String by option(help = "Secret from the NATS server (default: sup3rS3cr3t)", envvar = "NATS_SECRET")
        .default("sup3rS3cr3t")

    val typesPath: Path by option(help = "Path used for queue types (default: types)", envvar = "TYPE_PATH")
        .path()
        .default(Path.of("types"))

    val authKeyPath: Path by option(help = "Path to the gRPC auth secret (default: .secrets/auth.key)", envvar = "AUTH_KEY_PATH")
        .path()
        .default(Path.of(".secrets/auth.key"))

    // Values for the SimpleCloud API
    val networkId: String by option(help = "Your SimpleCloud Network ID (default: id)", envvar = "NETWORK_ID")
        .default("default")

    val networkSecret: String by option(help = "Your SimpleCloud Network Secret (default: sup3rS3cr3t)", envvar = "NETWORK_SECRET")
        .default("sup3rS3cr3t")

    val controllerUrl: String by option(help = "The URL from your SimpleCloud Controller (default: https://controller.simplecloud.app)", envvar = "CONTROLLER_URL")
        .default("https://controller.simplecloud.app")

    val controllerNatsUrl: String by option(help = "The URL from your SimpleCloud NATS server (default: wss://nats.simplecloud.app:443)", envvar = "CONTROLLER_NATS_URL")
        .default("wss://nats.simplecloud.app:443")

    override suspend fun run() {
        QueueRuntime(this).start()
    }

}
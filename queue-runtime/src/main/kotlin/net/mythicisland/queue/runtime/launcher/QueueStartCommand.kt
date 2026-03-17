package net.mythicisland.queue.runtime.launcher

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.sources.PropertiesValueSource
import com.github.ajalt.clikt.sources.ValueSource
import net.mythicisland.queue.runtime.QueueRuntime
import java.io.File
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object QueueStartCommand : SuspendingCliktCommand() {

    init {
        context {
            valueSource = PropertiesValueSource.from(File("queue.properties"), false, ValueSource.envvarKey())
        }
    }

    val databaseUrl: String by option(help = "Database URL", envvar = "DATABASE_URL")
        .default("")

    val grpcPort: Int by option(help = "gRPC Port", envvar = "GRPC_PORT")
        .int().default(4564)

    val natsUser: String by option(help = "NATS User", envvar = "NATS_USER")
        .default("your-nats-user")

    val natsSecret: String by option(help = "NATS secret", envvar = "NATS_SECRET")
        .default("your-nats-secret")

    val natsUrl: String by option(help = "NATS URL", envvar = "NATS_URL")
        .default("nats://your-nats-url:4222")

    val natsFailoverReconnectAfter: Duration by option(help = "Force a full NATS reconnect after this reconnecting duration (e.g. 30s, 2m, 1h)", envvar = "NATS_FAILOVER_RECONNECT_AFTER")
        .convert { parseDuration(it) }
        .default(30.seconds)

    val configPath: Path by option(help = "Config path", envvar = "CONFIG_PATH")
        .path().default(Path.of(""))

    val networkId: String by option(help = "Simplecloud Network ID", envvar = "NETWORK_ID")
        .default("your-network-id")

    val networkSecret: String by option(help = "Simplecloud Network Secret", envvar = "NETWORK_SECRET")
        .default("your-network-secret")

    val controllerUrl: String by option(help = "Simplecloud Controller URL", envvar = "CONTROLLER_URL")
        .default("https://controller.platform.simplecloud.app")

    val controllerNatsUrl: String by option(help = "Simplecloud Controller Nats URL", envvar = "CONTROLLER_NATS_URL")
        .default("nats://platform.simplecloud.app:4222")

    override suspend fun run() {
        QueueRuntime(this).start()
    }

    private fun parseDuration(value: String): Duration {
        val trimmed = value.trim().lowercase()
        val match = DURATION_PATTERN.matchEntire(trimmed)
            ?: throw BadParameterValue("Invalid duration '$value'. Expected format like '30s', '2m', '500ms', '1h'.")

        val amount = match.groupValues[1].toLongOrNull()
            ?: throw BadParameterValue("Invalid duration value '$value'.")
        val unit = match.groupValues[2]

        return when (unit) {
            "ms" -> amount.milliseconds
            "s" -> amount.seconds
            "m" -> amount.minutes
            "h" -> amount.hours
            else -> throw BadParameterValue("Unsupported duration unit in '$value'. Use ms, s, m, or h.")
        }
    }

    private val DURATION_PATTERN = Regex("^(\\d+)(ms|s|m|h)$")

}
package net.mythicisland.queue.shared.type

import build.buf.gen.mythicisland.queue.v1.exclusiveMode
import build.buf.gen.mythicisland.queue.v1.matchConfig
import build.buf.gen.mythicisland.queue.v1.partyConfig
import build.buf.gen.mythicisland.queue.v1.queueType
import build.buf.gen.mythicisland.queue.v1.serverConfig
import build.buf.gen.mythicisland.queue.v1.sharedMode
import build.buf.gen.mythicisland.queue.v1.teamConfig
import com.google.protobuf.duration
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.time.Duration
import build.buf.gen.mythicisland.queue.v1.MatchConfig as MatchConfigProto
import build.buf.gen.mythicisland.queue.v1.QueueType as QueueTypeProto
import build.buf.gen.mythicisland.queue.v1.ServerConfig as ServerConfigProto
import build.buf.gen.mythicisland.queue.v1.SharedMode as SharedModeProto
import build.buf.gen.mythicisland.queue.v1.TeamConfig as TeamConfigProto
import com.google.protobuf.Duration as DurationProto

@ConfigSerializable
data class QueueType(
    val name: String = "",
    val server: ServerConfig = ServerConfig(),
    val match: MatchConfig = MatchConfig(),
    val properties: Map<String, String> = emptyMap(),
) {

    fun toProto(): QueueTypeProto = queueType {
        name = this@QueueType.name
        server = this@QueueType.server.toProto()
        match = this@QueueType.match.toProto()
        properties.putAll(this@QueueType.properties)
    }

    companion object {
        fun fromProto(proto: QueueTypeProto): QueueType = QueueType(
            name = proto.name,
            server = ServerConfig.fromProto(proto.server),
            match = MatchConfig.fromProto(proto.match),
            properties = proto.propertiesMap.toMap(),
        )
    }

}

@ConfigSerializable
data class ServerConfig(
    val group: String = "",
    val allocationTimeout: Duration = Duration.ofSeconds(60),
    val shared: SharedMode? = null,
) {

    fun toProto(): ServerConfigProto = serverConfig {
        group = this@ServerConfig.group
        allocationTimeout = this@ServerConfig.allocationTimeout.toProto()
        if (this@ServerConfig.shared == null) {
            exclusive = exclusiveMode {}
        } else {
            shared = this@ServerConfig.shared.toProto()
        }
    }

    companion object {
        fun fromProto(proto: ServerConfigProto): ServerConfig = ServerConfig(
            group = proto.group,
            allocationTimeout = proto.allocationTimeout.toJavaDuration(),
            shared = if (proto.hasShared()) SharedMode.fromProto(proto.shared) else null,
        )
    }

}

@ConfigSerializable
data class SharedMode(
    val acceptTimeout: Duration = Duration.ofSeconds(10),
    val capacity: Int? = null,
) {

    fun toProto(): SharedModeProto = sharedMode {
        acceptTimeout = this@SharedMode.acceptTimeout.toProto()
        if (this@SharedMode.capacity != null) {
            capacity = this@SharedMode.capacity
        }
    }

    companion object {
        fun fromProto(proto: SharedModeProto): SharedMode = SharedMode(
            acceptTimeout = proto.acceptTimeout.toJavaDuration(),
            capacity = if (proto.hasCapacity()) proto.capacity else null,
        )
    }

}

@ConfigSerializable
data class MatchConfig(
    val minPlayers: Int = 0,
    val maxPlayers: Int? = null,
    val fillTime: Duration = Duration.ZERO,
    val countdown: Duration = Duration.ZERO,
    val teams: TeamConfig? = null,
    val parties: PartyConfig = PartyConfig(),
) {

    val effectiveMaxPlayers: Int
        get() = if (teams != null) teams.count * teams.size else maxPlayers ?: 0

    val effectivePartySize: Int
        get() = parties.maxSize ?: (teams?.size ?: effectiveMaxPlayers)

    fun toProto(): MatchConfigProto = matchConfig {
        minPlayers = this@MatchConfig.minPlayers
        maxPlayers = this@MatchConfig.effectiveMaxPlayers
        fillTime = this@MatchConfig.fillTime.toProto()
        countdown = this@MatchConfig.countdown.toProto()
        if (this@MatchConfig.teams != null) {
            teams = this@MatchConfig.teams.toProto()
        }
        parties = partyConfig { maxSize = this@MatchConfig.effectivePartySize }
    }

    companion object {
        fun fromProto(proto: MatchConfigProto): MatchConfig = MatchConfig(
            minPlayers = proto.minPlayers,
            maxPlayers = if (proto.hasTeams()) null else proto.maxPlayers,
            fillTime = proto.fillTime.toJavaDuration(),
            countdown = proto.countdown.toJavaDuration(),
            teams = if (proto.hasTeams()) TeamConfig.fromProto(proto.teams) else null,
            parties = PartyConfig(proto.parties.maxSize),
        )
    }

}

@ConfigSerializable
data class TeamConfig(
    val count: Int = 0,
    val size: Int = 0,
) {

    fun toProto(): TeamConfigProto = teamConfig {
        count = this@TeamConfig.count
        size = this@TeamConfig.size
    }

    companion object {
        fun fromProto(proto: TeamConfigProto): TeamConfig = TeamConfig(proto.count, proto.size)
    }

}

@ConfigSerializable
data class PartyConfig(
    val maxSize: Int? = null,
)

private fun Duration.toProto(): DurationProto = duration {
    seconds = this@toProto.seconds
    nanos = this@toProto.nano
}

private fun DurationProto.toJavaDuration(): Duration = Duration.ofSeconds(seconds, nanos.toLong())

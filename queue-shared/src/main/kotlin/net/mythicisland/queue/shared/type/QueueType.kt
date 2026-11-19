package net.mythicisland.queue.shared.type

import build.buf.gen.mythicisland.queue.v1.queueType
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import build.buf.gen.mythicisland.queue.v1.QueueType as QueueTypeProto

@ConfigSerializable
data class QueueType(
    val name: String = "",
    val server: ServerConfig = ServerConfig(),
    val match: MatchConfig = MatchConfig(),
    val properties: Map<String, String> = emptyMap(),
    val active: Boolean = true,
) {

    fun toProto(): QueueTypeProto = queueType {
        name = this@QueueType.name
        server = this@QueueType.server.toProto()
        match = this@QueueType.match.toProto()
        properties.putAll(this@QueueType.properties)
        active = this@QueueType.active
    }

    companion object {
        fun fromProto(proto: QueueTypeProto): QueueType = QueueType(
            name = proto.name,
            server = ServerConfig.fromProto(proto.server),
            match = MatchConfig.fromProto(proto.match),
            properties = proto.propertiesMap.toMap(),
            active = if (proto.hasActive()) proto.active else true,
        )
    }

}

package net.mythicisland.queue.shared.type

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class PartyConfig(
    val maxSize: Int? = null,
)

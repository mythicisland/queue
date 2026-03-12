package net.mythicisland.queue.runtime.config

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MessageConfig(
    val variables: Map<String, String> = mapOf(),
) {
    private val miniMessage = MiniMessage.miniMessage()

    private fun tagResolver(): TagResolver {
        val resolvers = variables.map { (key, value) ->
            TagResolver.resolver(key, Tag.selfClosingInserting(miniMessage.deserialize(value)))
        }
        return TagResolver.resolver(*resolvers.toTypedArray())
    }

    fun send(message: String, vararg tagResolver: TagResolver): Component =
        miniMessage.deserialize(message, TagResolver.resolver(tagResolver(), *tagResolver))
}
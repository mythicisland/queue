package net.mythicisland.queue.runtime.queue.visualizer

import app.simplecloud.api.server.Server
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object ServerTagResolver {

    fun get(server: Server): TagResolver {
        return TagResolver.resolver(
            TagResolver.resolver("server_id", Tag.selfClosingInserting(Component.text(server.serverId))),
            TagResolver.resolver("server_numerical_id", Tag.selfClosingInserting(Component.text(server.numericalId))),
            TagResolver.resolver("server_group_name", Tag.selfClosingInserting(Component.text(server.group.name))),
            TagResolver.resolver("server_name", Tag.selfClosingInserting(Component.text("${server.group.name}-${server.numericalId}"))),
            TagResolver.resolver("server_state", Tag.selfClosingInserting(Component.text(server.state.toString()))),
            TagResolver.resolver("server_ip", Tag.selfClosingInserting(Component.text(server.ip))),
            TagResolver.resolver("server_port", Tag.selfClosingInserting(Component.text(server.port))),
            TagResolver.resolver("server_online_players", Tag.selfClosingInserting(Component.text(server.playerCount))),
            TagResolver.resolver("server_max_players", Tag.selfClosingInserting(Component.text(server.maxPlayers))),
            TagResolver.resolver("server_min_memory", Tag.selfClosingInserting(Component.text(server.minMemory))),
            TagResolver.resolver("server_max_memory", Tag.selfClosingInserting(Component.text(server.maxMemory))),
        )
    }
}
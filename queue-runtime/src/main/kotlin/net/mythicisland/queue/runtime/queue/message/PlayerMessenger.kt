package net.mythicisland.queue.runtime.queue.message

import app.simplecloud.api.player.PlayerApi
import net.kyori.adventure.text.minimessage.MiniMessage
import net.mythicisland.queue.runtime.extension.asPlayerOrNull
import org.apache.logging.log4j.LogManager
import java.util.UUID

class PlayerMessenger(
    private val playerApi: PlayerApi
) {

    private val logger = LogManager.getLogger(PlayerMessenger::class.java)
    private val miniMessage = MiniMessage.miniMessage()

    suspend fun send(playerIds: List<UUID>, message: String) {
        val component = miniMessage.deserialize(message)
        for (playerId in playerIds) {
            try {
                val player = playerId.asPlayerOrNull(playerApi) ?: continue
                player.sendMessage(component)
            } catch (e: Exception) {
                logger.debug("Failed to send message to player {}: {}", playerId, e.message)
            }
        }
    }
}
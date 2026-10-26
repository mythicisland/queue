package net.mythicisland.queue.runtime.transfer

import app.simplecloud.api.CloudApi
import app.simplecloud.api.player.CloudPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import java.util.UUID

class PlayerTransferService(private val api: CloudApi) {

    private val logger = LoggerFactory.getLogger(PlayerTransferService::class)

    suspend fun transfer(players: List<UUID>, serverName: String): TransferResult = coroutineScope {
        val results = players.map { player -> async { player to connect(player, serverName) } }.awaitAll()
        val (transferred, missing) = results.partition { it.second }
        TransferResult(transferred.map { it.first }, missing.map { it.first })
    }

    private suspend fun connect(player: UUID, serverName: String): Boolean {
        val result = try {
            api.player().connect(player, serverName).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Failed to transfer player", Field("player", player), Field("server", serverName), Field.error(e))
            return false
        }

        if (result == CloudPlayer.ConnectResult.SUCCESS || result == CloudPlayer.ConnectResult.ALREADY_CONNECTED) {
            return true
        }

        logger.warn("Failed to transfer player", Field("player", player), Field("server", serverName), Field("result", result))
        return false
    }

}

package net.mythicisland.queue.runtime.extension

import app.simplecloud.api.player.CloudPlayer
import app.simplecloud.api.player.PlayerApi
import kotlinx.coroutines.future.await
import java.util.UUID

suspend fun UUID.asPlayer(api: PlayerApi): CloudPlayer {
    return api.get(this).await()
        ?: throw NoSuchElementException("Player with UUID $this is not online")
}

suspend fun UUID.asPlayerOrNull(api: PlayerApi): CloudPlayer? {
    return api.get(this).await()
}

suspend fun String.asPlayer(api: PlayerApi): CloudPlayer? {
    return api.get(this).await()
}
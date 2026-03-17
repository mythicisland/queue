package net.mythicisland.queue.shared.extension

import app.simplecloud.api.player.CloudPlayer
import app.simplecloud.api.player.PlayerApi
import kotlinx.coroutines.future.await
import java.util.UUID

suspend fun UUID.asPlayerOrNull(api: PlayerApi): CloudPlayer? {
    return api.get(this).await()
}
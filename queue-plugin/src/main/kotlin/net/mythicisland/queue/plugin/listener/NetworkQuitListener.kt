package net.mythicisland.queue.plugin.listener

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.mythicisland.queue.api.QueueApi

class NetworkQuitListener(
    private val api: QueueApi
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        val player = event.player

        scope.launch {
            try {
                api.player().dequeue(player.uniqueId).await()
            } catch (_: Exception) {

            }
        }
    }
}
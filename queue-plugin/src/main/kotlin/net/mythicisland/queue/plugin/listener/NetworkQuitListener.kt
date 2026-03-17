package net.mythicisland.queue.plugin.listener

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.mythicisland.queue.api.QueueApi
import net.mythicisland.queue.api.extensions.dequeueSuspending

class NetworkQuitListener(
    private val api: QueueApi,
    private val scope: CoroutineScope,
) {

    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        val player = event.player

        scope.launch {
            try {
                api.player().dequeueSuspending(player.uniqueId)
            } catch (_: Exception) {

            }
        }
    }
}
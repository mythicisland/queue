package net.mythicisland.common.nats

import io.nats.client.Connection
import io.nats.client.ConnectionListener
import org.apache.logging.log4j.LogManager
import java.util.concurrent.CopyOnWriteArrayList

class NatsConnectionHandler : ConnectionListener {

    private val callbacks = CopyOnWriteArrayList<NatsConnectionStateCallback>()
    private var lastConnectionState: Boolean? = null

    @Deprecated("Deprecated in Java")
    override fun connectionEvent(conn: Connection, type: ConnectionListener.Events) {
        when (type) {
            ConnectionListener.Events.CONNECTED -> {
                logger.info("Connected to NATS server (${conn.connectedUrl})")
                notifyCallbacks(connected = true)
            }

            ConnectionListener.Events.DISCONNECTED -> {
                logger.warn("Disconnected from NATS server")
                notifyCallbacks(connected = false)
            }

            ConnectionListener.Events.RECONNECTED -> {
                logger.info("Reconnected to NATS server (${conn.connectedUrl})")
                notifyCallbacks(connected = true)
            }

            ConnectionListener.Events.CLOSED -> {
                logger.error("Connection to NATS server closed")
                notifyCallbacks(connected = false)
            }

            else -> {}
        }
    }

    private fun notifyCallbacks(connected: Boolean) {
        if (lastConnectionState == connected) {
            return
        }
        lastConnectionState = connected
        for (callback in callbacks) {
            try {
                callback.onConnectionStateChanged(connected)
            } catch (e: Exception) {
                logger.error("Error notifying connection state callback", e)
            }
        }
    }

    companion object {
        private val logger = LogManager.getLogger(NatsConnectionHandler::class.java)
    }

}
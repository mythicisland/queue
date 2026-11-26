package net.mythicisland.queue.runtime.nats

import io.nats.client.Connection
import io.nats.client.ConnectionListener
import io.nats.client.Consumer
import io.nats.client.ErrorListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

class NatsListener : ConnectionListener, ErrorListener {

    private val logger = LoggerFactory.getLogger(NatsListener::class)
    private val connected = AtomicBoolean(false)
    private val reconnected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val reconnects: SharedFlow<Unit> get() = reconnected

    @Deprecated("Deprecated in Java")
    override fun connectionEvent(conn: Connection, type: ConnectionListener.Events) {
        when (type) {
            ConnectionListener.Events.CONNECTED -> {
                connected.set(true)
                logger.info("Connected to NATS", Field("server", conn.connectedUrl))
            }
            ConnectionListener.Events.RECONNECTED -> {
                connected.set(true)
                logger.info("Reconnected to NATS", Field("server", conn.connectedUrl))
                reconnected.tryEmit(Unit)
            }
            ConnectionListener.Events.DISCONNECTED -> {
                connected.set(false)
                logger.warn("Disconnected from NATS, reconnecting")
            }
            ConnectionListener.Events.LAME_DUCK -> logger.warn("NATS server is shutting down")
            ConnectionListener.Events.CLOSED -> logger.info("NATS connection closed")
            else -> return
        }
    }

    override fun errorOccurred(conn: Connection, error: String) {
        logger.error("NATS reported an error", Field("error", error))
    }

    override fun exceptionOccurred(conn: Connection, exp: Exception) {
        if (connected.get()) {
            logger.warn("NATS connection error", Field.error(exp))
        }
    }

    override fun slowConsumerDetected(conn: Connection, consumer: Consumer) {
        logger.warn("NATS consumer is too slow and drops messages", Field("pending", consumer.pendingMessageCount))
    }

}

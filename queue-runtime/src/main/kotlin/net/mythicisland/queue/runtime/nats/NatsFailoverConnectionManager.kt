package net.mythicisland.queue.runtime.nats

import io.nats.client.Connection
import io.nats.client.ErrorListener
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class NatsFailoverConnectionManager(
    private val natsUrl: String,
    private val networkId: String,
    private val networkSecret: String,
    private val errorListener: ErrorListener,
    private val connectionHandler: NatsConnectionHandler,
    private val failoverReconnectAfter: Duration,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectionRef = AtomicReference(createNatsConnection())
    private val reconnecting = AtomicBoolean(false)
    private val connectionCallbacks = CopyOnWriteArrayList<(Connection) -> Unit>()
    private val connectionProxy: Connection = createConnectionProxy()

    private val monitorJob: Job = scope.launch {
        monitorConnection()
    }

    fun connection(): Connection {
        return connectionProxy
    }

    fun shutdown() {
        monitorJob.cancel()
        scope.cancel()
        runCatching {
            connectionRef.get().close()
        }
    }

    private fun createConnectionProxy(): Connection {
        val proxy = Proxy.newProxyInstance(
            Connection::class.java.classLoader,
            arrayOf(Connection::class.java)
        ) { _, method, args ->
            val target = connectionRef.get()
            try {
                method.invoke(target, *(args ?: emptyArray()))
            } catch (e: InvocationTargetException) {
                throw (e.targetException ?: e)
            }
        }

        return proxy as Connection
    }

    private suspend fun monitorConnection() {
        var reconnectingSinceMillis: Long? = null

        while (true) {
            val conn = connectionRef.get()
            when (val status = conn.status) {
                Connection.Status.CLOSED -> {
                    reconnectingSinceMillis = null
                    reconnectCompletely("connection is CLOSED")
                }

                Connection.Status.RECONNECTING, Connection.Status.DISCONNECTED -> {
                    val since = reconnectingSinceMillis ?: System.currentTimeMillis().also {
                        reconnectingSinceMillis = it
                    }

                    if (failoverReconnectAfter > Duration.ZERO &&
                        System.currentTimeMillis() - since >= failoverReconnectAfter.inWholeMilliseconds
                    ) {
                        reconnectingSinceMillis = null
                        reconnectCompletely(
                            "connection stayed $status for at least $failoverReconnectAfter"
                        )
                    }
                }

                else -> reconnectingSinceMillis = null
            }

            delay(2.seconds)
        }
    }

    private suspend fun reconnectCompletely(reason: String) {
        if (!reconnecting.compareAndSet(false, true)) {
            return
        }

        try {
            logger.warn("Forcing full NATS reconnect: $reason")

            var backoff = 1.seconds
            val maxBackoff = 30.seconds
            while (true) {
                val newConnection = runCatching {
                    createNatsConnection()
                }.getOrElse { error ->
                    logger.warn(
                        "Failed to establish new NATS connection, retrying in $backoff: ${error.message}"
                    )
                    null
                }

                if (newConnection != null) {
                    val previousConnection = connectionRef.getAndSet(newConnection)
                    if (previousConnection !== newConnection) {
                        runCatching {
                            previousConnection.close()
                        }.onFailure {
                            logger.debug("Failed to close previous NATS connection after successful failover reconnect", it)
                        }
                    }
                    logger.info("Successfully established a new NATS connection after forced failover reconnect")
                    notifyConnectionCallbacks(newConnection)
                    return
                }

                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(maxBackoff)
            }
        } finally {
            reconnecting.set(false)
        }
    }

    private fun notifyConnectionCallbacks(connection: Connection) {
        for (callback in connectionCallbacks) {
            runCatching {
                callback(connection)
            }.onFailure {
                logger.error("Failed to execute NATS connection callback", it)
            }
        }
    }

    private fun createNatsConnection(): Connection {
        return Nats.connect(
            Options.builder()
                .server(natsUrl)
                .userInfo(networkId, networkSecret)
                .maxReconnects(-1)
                .errorListener(errorListener)
                .connectionListener(connectionHandler)
                .build()
        )
    }

    companion object {
        private val logger = LogManager.getLogger(NatsFailoverConnectionManager::class.java)
    }
}
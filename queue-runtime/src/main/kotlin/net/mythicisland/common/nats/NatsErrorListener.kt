package net.mythicisland.common.nats

import io.nats.client.Connection
import io.nats.client.Consumer
import io.nats.client.ErrorListener
import io.nats.client.ErrorListener.FlowControlSource
import io.nats.client.JetStreamSubscription
import io.nats.client.Message
import io.nats.client.support.Status
import org.apache.logging.log4j.LogManager
import java.net.ConnectException

class NatsErrorListener : ErrorListener {

    override fun errorOccurred(conn: Connection?, error: String?) {
        logger.error(supplyMessage("errorOccurred", conn, null, null, "Error: ", error))
    }

    override fun exceptionOccurred(conn: Connection?, exp: Exception?) {
        if (exp is ConnectException) {
            logger.error(supplyMessage("exceptionOccurred", conn, null, null, "Connection Exception: ", exp.message))
            return
        }
        logger.error(supplyMessage("exceptionOccurred", conn, null, null, "Exception: ", exp), exp)
    }

    override fun slowConsumerDetected(conn: Connection?, consumer: Consumer?) {
        logger.warn(supplyMessage("slowConsumerDetected", conn, consumer, null))
    }

    override fun messageDiscarded(conn: Connection?, msg: Message?) {
        logger.info(supplyMessage("messageDiscarded", conn, null, null, "Message: ", msg))
    }

    override fun heartbeatAlarm(
        conn: Connection?, sub: JetStreamSubscription?,
        lastStreamSequence: Long, lastConsumerSequence: Long
    ) {
        logger.error(
            supplyMessage(
                "heartbeatAlarm",
                conn,
                null,
                sub,
                "lastStreamSequence: ",
                lastStreamSequence,
                "lastConsumerSequence: ",
                lastConsumerSequence
            )
        )
    }

    override fun unhandledStatus(conn: Connection?, sub: JetStreamSubscription?, status: Status?) {
        logger.warn(supplyMessage("unhandledStatus", conn, null, sub, "Status: ", status))
    }

    override fun pullStatusWarning(conn: Connection?, sub: JetStreamSubscription?, status: Status?) {
        logger.warn(supplyMessage("pullStatusWarning", conn, null, sub, "Status: ", status))
    }

    override fun pullStatusError(conn: Connection?, sub: JetStreamSubscription?, status: Status?) {
        logger.error(supplyMessage("pullStatusError", conn, null, sub, "Status: ", status))
    }

    override fun flowControlProcessed(
        conn: Connection?,
        sub: JetStreamSubscription?,
        id: String?,
        source: FlowControlSource?
    ) {
        logger.info(supplyMessage("flowControlProcessed", conn, null, sub, "FlowControlSource: ", source))
    }

    override fun socketWriteTimeout(conn: Connection?) {
        logger.error(supplyMessage("socketWriteTimeout", conn, null, null))
    }

    companion object {
        private val logger = LogManager.getLogger(NatsErrorListener::class.java)
    }

}
package net.mythicisland.queue.api

import net.mythicisland.queue.api.data.QueueDataApi
import net.mythicisland.queue.api.event.EventApi
import net.mythicisland.queue.api.internal.QueueApiImpl
import net.mythicisland.queue.api.match.MatchApi
import net.mythicisland.queue.api.ticket.TicketApi

/**
 * Entrypoint for talking to the queue.
 */
interface QueueApi : AutoCloseable {

    /**
     * Creating and deleting tickets.
     */
    fun ticket(): TicketApi

    /**
     * Accepting, rejecting and releasing matches, used by game servers.
     */
    fun match(): MatchApi

    /**
     * Reading tickets, matches, queue types and stats.
     */
    fun data(): QueueDataApi

    /**
     * Subscribing to ticket and match events.
     */
    fun event(): EventApi

    /**
     * Closes the gRPC channel and the NATS connection.
     */
    override fun close()

    companion object {

        /**
         * Creates a [QueueApi], [options] default to environment variables.
         */
        @JvmStatic
        fun create(options: QueueApiOptions = QueueApiOptions()): QueueApi = QueueApiImpl(options)
    }

}

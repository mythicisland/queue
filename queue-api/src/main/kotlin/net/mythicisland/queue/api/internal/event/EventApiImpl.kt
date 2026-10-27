package net.mythicisland.queue.api.internal.event

import io.nats.client.Connection
import net.mythicisland.queue.api.event.EventApi
import net.mythicisland.queue.api.event.match.MatchEventApi
import net.mythicisland.queue.api.event.ticket.TicketEventApi
import net.mythicisland.queue.api.internal.event.match.MatchEventApiImpl
import net.mythicisland.queue.api.internal.event.ticket.TicketEventApiImpl

internal class EventApiImpl(private val connection: Connection) : EventApi {

    private val ticketEventApi = TicketEventApiImpl(connection)
    private val matchEventApi = MatchEventApiImpl(connection, null)

    override fun ticket(): TicketEventApi = ticketEventApi

    override fun match(): MatchEventApi = matchEventApi

    override fun match(serverId: String): MatchEventApi = MatchEventApiImpl(connection, serverId)

}

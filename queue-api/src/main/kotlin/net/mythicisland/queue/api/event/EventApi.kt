package net.mythicisland.queue.api.event

import net.mythicisland.queue.api.event.match.MatchEventApi
import net.mythicisland.queue.api.event.ticket.TicketEventApi

/**
 * Subscribing to queue events.
 *
 * Every handler gets its own [Subscription], keep it to unsubscribe later.
 *
 * ```
 * val subscription = api.event().ticket().onStateChanged { event ->
 *     println("${event.ticket.id} is now ${event.ticket.state}")
 * }
 *
 * subscription.unsubscribe()
 * ```
 */
interface EventApi {

    /**
     * Events of all tickets.
     */
    fun ticket(): TicketEventApi

    /**
     * Events of all matches.
     */
    fun match(): MatchEventApi

    /**
     * Events of the matches running on [serverId]. A match only shows up here once it has a server,
     * and its last state change shows up here when an offer to [serverId] is withdrawn.
     */
    fun match(serverId: String): MatchEventApi

}

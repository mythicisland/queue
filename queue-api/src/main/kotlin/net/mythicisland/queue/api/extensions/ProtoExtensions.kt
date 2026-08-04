package net.mythicisland.queue.api.extensions

import net.mythicisland.queue.api.internal.ProtoUtil
import net.mythicisland.queue.api.match.Match
import net.mythicisland.queue.api.ticket.Ticket

/**
 * Maps the protobuf ticket to the API type.
 *
 * The gRPC services answer with protobuf messages, everything else in the API
 * works with [Ticket] and [Match]. These two turn one into the other.
 */
fun build.buf.gen.mythicisland.queue.v2.Ticket.toApi(): Ticket = ProtoUtil.toTicket(this)

/**
 * Maps the protobuf match to the API type.
 */
fun build.buf.gen.mythicisland.queue.v2.Match.toApi(): Match = ProtoUtil.toMatch(this)

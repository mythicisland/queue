package net.mythicisland.queue.shared.message

import build.buf.gen.mythicisland.queue.v1.QueueStatus
import net.mythicisland.queue.shared.extension.toMiniFont

typealias Messages = Map<QueueStatus, String>

val defaultMessages: Messages = mapOf(
    QueueStatus.NOT_ENOUGH_PLAYERS to "<color:#cbd5e1>Waiting for players (<queue_players>/<queue_max_capacity>)".toMiniFont(),
    QueueStatus.WAITING_COUNTDOWN to ("<color:#22c55e>Found Match!</color> <color:#cbd5e1>Starting in <queue_waiting_countdown_seconds>s (<queue_players>/<queue_max_capacity>)").toMiniFont(),
    QueueStatus.SEARCHING_SERVER to "<color:#cbd5e1>Searching for a <queue_type> server (<queue_players>/<queue_max_capacity>)".toMiniFont(),
    QueueStatus.WAITING_FOR_SERVER to "<color:#cbd5e1>Waiting for a <queue_type> server (<queue_players>/<queue_max_capacity>)".toMiniFont(),
    QueueStatus.SERVER_READY to ("<color:#22c55e>Server found!</color> <color:#cbd5e1>(<queue_players>/<queue_max_capacity>)").toMiniFont(),
    QueueStatus.COUNTDOWN to "<color:#cbd5e1>Game starting in <queue_countdown_seconds> seconds (<queue_players>/<queue_max_capacity>)".toMiniFont(),
    QueueStatus.TELEPORTING to "<color:#cbd5e1>Teleporting to <server_name>...".toMiniFont(),
)
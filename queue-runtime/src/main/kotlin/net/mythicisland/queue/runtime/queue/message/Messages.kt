package net.mythicisland.queue.runtime.queue.message

import build.buf.gen.mythicisland.queue.v1.QueueStatus

typealias Messages = Map<QueueStatus, String>

val defaultMessages: Messages = mapOf(
    QueueStatus.NOT_ENOUGH_PLAYERS to "Waiting for players (<queue_players>/<queue_max_capacity>)",
    QueueStatus.WAITING_COUNTDOWN to "Starting in <queue_waiting_countdown_seconds>s (<queue_players>/<queue_max_capacity>)",
    QueueStatus.SEARCHING_SERVER to "Searching for a <queue_type> server (<queue_players>/<queue_max_capacity>)",
    QueueStatus.WAITING_FOR_SERVER to "Waiting for a <queue_type> server (<queue_players>/<queue_max_capacity>)",
    QueueStatus.SERVER_READY to "Server found! (<queue_players>/<queue_max_capacity>)",
    QueueStatus.COUNTDOWN to "Game starting in <queue_countdown_seconds> seconds (<queue_players>/<queue_max_capacity>)",
    QueueStatus.TELEPORTING to "Teleporting to <server_name>...",
)
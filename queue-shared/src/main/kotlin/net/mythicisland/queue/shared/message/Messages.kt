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

object CommandMessages {
    const val ENQUEUE_SUCCESS = "<color:#22c55e>You have joined the queue"
    const val ENQUEUE_NOT_FOUND = "<color:#dc2626>This queue type does not exist"
    const val ENQUEUE_ALREADY_QUEUED = "<color:#dc2626>You are already in a queue"
    const val ENQUEUE_FAILED = "<color:#dc2626>Failed to join the queue"
    const val DEQUEUE_SUCCESS = "<color:#22c55e>You have left the queue"
    const val DEQUEUE_NOT_IN_QUEUE = "<color:#dc2626>You are not in a queue"
    const val DEQUEUE_FAILED = "<color:#dc2626>Failed to leave the queue"
}
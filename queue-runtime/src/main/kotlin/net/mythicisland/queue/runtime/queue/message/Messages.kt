package net.mythicisland.queue.runtime.queue.message

import build.buf.gen.mythicisland.queue.v1.QueueStatus
import net.mythicisland.queue.runtime.extension.toMiniFont

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
    val ENQUEUE_SUCCESS = "<color:#22c55e>You have joined the queue".toMiniFont()
    val ENQUEUE_NOT_FOUND = "<color:#dc2626>This queue type does not exist".toMiniFont()
    val ENQUEUE_ALREADY_QUEUED = "<color:#dc2626>You are already in a queue".toMiniFont()
    val ENQUEUE_FAILED = "<color:#dc2626>Failed to join the queue".toMiniFont()
    val DEQUEUE_SUCCESS = "<color:#22c55e>You have left the queue".toMiniFont()
    val DEQUEUE_NOT_IN_QUEUE = "<color:#dc2626>You are not in a queue".toMiniFont()
    val DEQUEUE_FAILED = "<color:#dc2626>Failed to leave the queue".toMiniFont()
}
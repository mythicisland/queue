package net.mythicisland.queue.runtime.launcher

import com.github.ajalt.clikt.command.main

suspend fun main(args: Array<String>) {
    QueueStartCommand.main(args)
}
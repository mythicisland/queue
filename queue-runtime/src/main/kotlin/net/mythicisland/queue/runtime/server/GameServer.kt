package net.mythicisland.queue.runtime.server

import app.simplecloud.api.server.ServerState

data class GameServer(
    val id: String,
    val name: String,
    val group: String,
    val state: ServerState,
    val capacity: Int?,
    val matches: Set<String>,
    val hosted: Set<String> = emptySet(),
) {

    val freeCapacity: Int
        get() = (capacity ?: 0) - matches.size

}

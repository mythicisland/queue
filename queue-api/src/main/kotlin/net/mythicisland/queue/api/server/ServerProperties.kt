package net.mythicisland.queue.api.server

/**
 * SimpleCloud server properties the queue reads and writes.
 */
object ServerProperties {

    /**
     * How many matches a shared server can host at once, set by the game server.
     *
     * Overrides `server.shared.capacity` of the queue type.
     */
    const val CAPACITY = "queue.capacity"

    /**
     * Ids of the matches whose players are on the server, written by the queue. Read it, but don't overwrite it.
     */
    const val MATCHES = "queue.matches"

}

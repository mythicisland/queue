package net.mythicisland.queue.runtime.server

import app.simplecloud.api.server.ServerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerAllocatorTest {

    @Test
    fun `exclusive takes an available server`() {
        val servers = listOf(server("a", ServerState.INGAME), server("b", ServerState.AVAILABLE))

        assertEquals("b", ServerAllocator.chooseExclusive(servers, emptySet())?.id)
    }

    @Test
    fun `exclusive skips servers that already host a match`() {
        val servers = listOf(server("a", ServerState.AVAILABLE), server("b", ServerState.AVAILABLE))

        assertEquals("b", ServerAllocator.chooseExclusive(servers, setOf("a"))?.id)
    }

    @Test
    fun `exclusive finds nothing without available servers`() {
        assertNull(ServerAllocator.chooseExclusive(listOf(server("a", ServerState.STARTING)), emptySet()))
    }

    @Test
    fun `shared needs enough servers for all waiting matches`() {
        assertEquals(3, ServerAllocator.serversNeeded(9, 4))
    }

    @Test
    fun `shared starts one server at a time while its capacity is unknown`() {
        assertEquals(1, ServerAllocator.serversNeeded(9, null))
    }

    @Test
    fun `shared falls back to one server for an invalid capacity`() {
        assertEquals(1, ServerAllocator.serversNeeded(9, 0))
    }

    @Test
    fun `shared prefers the fullest server with free capacity`() {
        val servers = listOf(
            server("empty", capacity = 4),
            server("half", capacity = 4, matches = 2),
            server("full", capacity = 4, matches = 4),
        )

        assertEquals("half", ServerAllocator.chooseShared(servers, emptySet())?.id)
    }

    @Test
    fun `shared ignores servers without capacity`() {
        val servers = listOf(server("unset"), server("closed", capacity = 0))

        assertNull(ServerAllocator.chooseShared(servers, emptySet()))
    }

    @Test
    fun `shared never offers to a server that rejected the match`() {
        val servers = listOf(server("rejected", capacity = 4, matches = 3), server("other", capacity = 4))

        assertEquals("other", ServerAllocator.chooseShared(servers, setOf("rejected"))?.id)
    }

    @Test
    fun `shared ignores stopping servers`() {
        assertNull(ServerAllocator.chooseShared(listOf(server("a", ServerState.STOPPING, capacity = 4)), emptySet()))
    }

    private fun server(
        id: String,
        state: ServerState = ServerState.AVAILABLE,
        capacity: Int? = null,
        matches: Int = 0,
    ): GameServer = GameServer(
        id = id,
        name = "game-$id",
        group = "game",
        state = state,
        capacity = capacity,
        matches = List(matches) { "$id-match-$it" }.toSet(),
    )

}

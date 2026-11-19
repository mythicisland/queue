package net.mythicisland.queue.runtime.type

import net.mythicisland.queue.shared.type.MatchConfig
import net.mythicisland.queue.shared.type.PartyConfig
import net.mythicisland.queue.shared.type.QueueType
import net.mythicisland.queue.shared.type.ServerConfig
import net.mythicisland.queue.shared.type.TeamConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueueTypeChangesTest {

    @Test
    fun `unchanged queue types are not written again`() {
        val types = listOf(type("ffa"), type("teams"))

        assertTrue(QueueTypeChanges.between(types, types).isEmpty())
    }

    @Test
    fun `a stored queue type with the same effective config counts as unchanged`() {
        val file = type("teams")
        val stored = QueueType.fromProto(file.toProto())

        assertTrue(QueueTypeChanges.between(listOf(stored), listOf(file)).isEmpty())
    }

    @Test
    fun `new and changed files are written`() {
        val current = listOf(type("ffa"))
        val wanted = listOf(type("ffa").copy(active = false), type("teams"))

        assertEquals(listOf("ffa", "teams"), QueueTypeChanges.between(current, wanted).changed.map { it.name })
    }

    @Test
    fun `queue types without a file are removed`() {
        val changes = QueueTypeChanges.between(listOf(type("ffa"), type("teams")), listOf(type("teams")))

        assertEquals(listOf("ffa"), changes.removed.map { it.name })
        assertTrue(changes.changed.isEmpty())
    }

    @Test
    fun `an empty directory removes every queue type`() {
        val changes = QueueTypeChanges.between(listOf(type("ffa"), type("teams")), emptyList())

        assertEquals(listOf("ffa", "teams"), changes.removed.map { it.name })
    }

    private fun type(name: String): QueueType = QueueType(
        name = name,
        server = ServerConfig(group = name),
        match = MatchConfig(minPlayers = 2, teams = TeamConfig(count = 2, size = 4), parties = PartyConfig()),
    )

}

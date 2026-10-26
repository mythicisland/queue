package net.mythicisland.queue.runtime.type

import net.mythicisland.queue.shared.type.MatchConfig
import net.mythicisland.queue.shared.type.PartyConfig
import net.mythicisland.queue.shared.type.QueueType
import net.mythicisland.queue.shared.type.ServerConfig
import net.mythicisland.queue.shared.type.TeamConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueueTypeValidatorTest {

    private val valid = QueueType(
        name = "bedwars",
        server = ServerConfig(group = "bedwars"),
        match = MatchConfig(minPlayers = 2, teams = TeamConfig(count = 2, size = 4)),
    )

    @Test
    fun `accepts a valid queue type`() {
        assertEquals(emptyList(), QueueTypeValidator.validate(valid))
    }

    @Test
    fun `accepts a queue type without teams`() {
        val type = valid.copy(match = MatchConfig(minPlayers = 2, maxPlayers = 8))

        assertEquals(emptyList(), QueueTypeValidator.validate(type))
    }

    @Test
    fun `requires a group`() {
        val type = valid.copy(server = ServerConfig(group = ""))

        assertEquals(1, QueueTypeValidator.validate(type).size)
    }

    @Test
    fun `rejects min players above the effective max players`() {
        val type = valid.copy(match = MatchConfig(minPlayers = 9, teams = TeamConfig(count = 2, size = 4)))

        assertEquals(1, QueueTypeValidator.validate(type).size)
    }

    @Test
    fun `rejects max players together with teams`() {
        val type = valid.copy(match = MatchConfig(minPlayers = 2, maxPlayers = 8, teams = TeamConfig(count = 2, size = 4)))

        assertEquals(1, QueueTypeValidator.validate(type).size)
    }

    @Test
    fun `requires max players or teams`() {
        val type = valid.copy(match = MatchConfig(minPlayers = 2))

        assertEquals(1, QueueTypeValidator.validate(type).size)
    }

    @Test
    fun `requires at least two teams`() {
        val type = valid.copy(match = MatchConfig(minPlayers = 1, teams = TeamConfig(count = 1, size = 4)))

        assertEquals(1, QueueTypeValidator.validate(type).size)
    }

    @Test
    fun `rejects a party size above the team size`() {
        val type = valid.copy(
            match = MatchConfig(minPlayers = 2, teams = TeamConfig(count = 2, size = 4), parties = PartyConfig(maxSize = 5)),
        )

        assertEquals(1, QueueTypeValidator.validate(type).size)
    }

    @Test
    fun `rejects a party size above max players without teams`() {
        val type = valid.copy(match = MatchConfig(minPlayers = 2, maxPlayers = 4, parties = PartyConfig(maxSize = 5)))

        assertEquals(1, QueueTypeValidator.validate(type).size)
    }

    @Test
    fun `collects every problem`() {
        val type = QueueType(name = "broken")

        assertTrue(QueueTypeValidator.validate(type).size >= 3)
    }

}

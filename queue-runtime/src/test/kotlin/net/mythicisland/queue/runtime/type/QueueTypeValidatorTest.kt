package net.mythicisland.queue.runtime.type

import net.mythicisland.queue.shared.type.MatchConfig
import net.mythicisland.queue.shared.type.MatchFilter
import net.mythicisland.queue.shared.type.MatchQuality
import net.mythicisland.queue.shared.type.MatchRule
import net.mythicisland.queue.shared.type.MatchRuleType
import net.mythicisland.queue.shared.type.PartyConfig
import net.mythicisland.queue.shared.type.QueueType
import net.mythicisland.queue.shared.type.ServerConfig
import net.mythicisland.queue.shared.type.SharedMode
import net.mythicisland.queue.shared.type.SpreadLimit
import net.mythicisland.queue.shared.type.TeamConfig
import net.mythicisland.queue.shared.type.Window
import java.time.Duration
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
    fun `rejects names that can not be used as keys`() {
        for (name in listOf("", "-bedwars", "bed wars", "bed.wars", "a".repeat(65))) {
            assertEquals(1, QueueTypeValidator.validate(valid.copy(name = name)).size, name)
        }
    }

    @Test
    fun `accepts names with letters, digits, dashes and underscores`() {
        assertEquals(emptyList(), QueueTypeValidator.validate(valid.copy(name = "Bedwars_4x4-ranked")))
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
    fun `rejects a group used by shared and exclusive queue types`() {
        val shared = valid.copy(name = "bedwars-shared", server = ServerConfig(group = "bedwars", shared = SharedMode()))

        assertEquals(1, QueueTypeValidator.validateGroups(listOf(valid, shared)).size)
    }

    @Test
    fun `accepts shared and exclusive queue types in different groups`() {
        val shared = valid.copy(name = "skywars", server = ServerConfig(group = "skywars", shared = SharedMode()))

        assertEquals(emptyList(), QueueTypeValidator.validateGroups(listOf(valid, shared)))
    }

    @Test
    fun `accepts valid match rules`() {
        val rules = listOf(
            MatchRule(type = MatchRuleType.EQUAL, key = "region", relaxAfter = Duration.ofSeconds(60)),
            MatchRule(type = MatchRuleType.RANGE, key = "rating", start = 100.0, widen = 50.0, every = Duration.ofSeconds(10), max = 500.0),
        )

        assertEquals(emptyList(), QueueTypeValidator.validate(withRules(rules)))
    }

    @Test
    fun `requires a type for every rule`() {
        assertEquals(1, QueueTypeValidator.validate(withRules(listOf(MatchRule(key = "region")))).size)
    }

    @Test
    fun `rejects range settings on an equal rule`() {
        val rule = MatchRule(type = MatchRuleType.EQUAL, key = "region", start = 100.0)

        assertEquals(1, QueueTypeValidator.validate(withRules(listOf(rule))).size)
    }

    @Test
    fun `requires start for a range rule`() {
        val rule = MatchRule(type = MatchRuleType.RANGE, key = "rating")

        assertEquals(1, QueueTypeValidator.validate(withRules(listOf(rule))).size)
    }

    @Test
    fun `requires every when a range rule widens`() {
        val rule = MatchRule(type = MatchRuleType.RANGE, key = "rating", start = 100.0, widen = 50.0)

        assertEquals(1, QueueTypeValidator.validate(withRules(listOf(rule))).size)
    }

    @Test
    fun `rejects a range max below start`() {
        val rule = MatchRule(type = MatchRuleType.RANGE, key = "rating", start = 100.0, max = 50.0)

        assertEquals(1, QueueTypeValidator.validate(withRules(listOf(rule))).size)
    }

    @Test
    fun `rejects two rules for the same key`() {
        val rule = MatchRule(type = MatchRuleType.EQUAL, key = "region")

        assertEquals(1, QueueTypeValidator.validate(withRules(listOf(rule, rule))).size)
    }

    @Test
    fun `collects every problem`() {
        val type = QueueType(name = "broken")

        assertTrue(QueueTypeValidator.validate(type).size >= 3)
    }

    @Test
    fun `rejects a max wait that is not positive`() {
        val type = valid.copy(match = valid.match.copy(maxWait = Duration.ZERO))

        assertEquals(1, QueueTypeValidator.validate(type).size)
    }

    @Test
    fun `rejects max failures below one`() {
        val type = valid.copy(match = valid.match.copy(maxFailures = 0))

        assertEquals(1, QueueTypeValidator.validate(type).size)
    }

    @Test
    fun `accepts valid match filters`() {
        val filters = listOf(
            MatchFilter(key = "version", values = listOf("1.21.4")),
            MatchFilter(key = "rating", min = 0.0, max = 1500.0),
            MatchFilter(key = "banned", present = false),
        )

        assertEquals(emptyList(), QueueTypeValidator.validate(withFilters(filters)))
    }

    @Test
    fun `rejects a filter with more than one condition`() {
        val filter = MatchFilter(key = "rating", values = listOf("1000"), min = 0.0)

        assertEquals(1, QueueTypeValidator.validate(withFilters(listOf(filter))).size)
    }

    @Test
    fun `rejects a filter without a condition`() {
        assertEquals(1, QueueTypeValidator.validate(withFilters(listOf(MatchFilter(key = "rating")))).size)
    }

    @Test
    fun `rejects a filter with empty values`() {
        val filter = MatchFilter(key = "version", values = emptyList())

        assertEquals(1, QueueTypeValidator.validate(withFilters(listOf(filter))).size)
    }

    @Test
    fun `rejects a filter min above max`() {
        val filter = MatchFilter(key = "rating", min = 1500.0, max = 0.0)

        assertEquals(1, QueueTypeValidator.validate(withFilters(listOf(filter))).size)
    }

    @Test
    fun `rejects two filters for the same key`() {
        val filter = MatchFilter(key = "region", values = listOf("eu"))

        assertEquals(1, QueueTypeValidator.validate(withFilters(listOf(filter, filter))).size)
    }

    @Test
    fun `accepts a valid match quality`() {
        val teams = TeamConfig(count = 2, size = 4, balanceBy = "rating")
        val quality = MatchQuality(
            spread = listOf(SpreadLimit(key = "rating", start = 200.0, widen = 50.0, every = Duration.ofSeconds(30), max = 600.0)),
            teamBalance = Window(start = 50.0),
        )
        val type = valid.copy(match = valid.match.copy(teams = teams, quality = quality))

        assertEquals(emptyList(), QueueTypeValidator.validate(type))
    }

    @Test
    fun `requires balance by for a team balance limit`() {
        val type = valid.copy(match = valid.match.copy(quality = MatchQuality(teamBalance = Window(start = 50.0))))

        assertEquals(1, QueueTypeValidator.validate(type).size)
    }

    @Test
    fun `requires start for a spread limit`() {
        val type = valid.copy(match = valid.match.copy(quality = MatchQuality(spread = listOf(SpreadLimit(key = "rating")))))

        assertEquals(1, QueueTypeValidator.validate(type).size)
    }

    @Test
    fun `rejects two spread limits for the same key`() {
        val limit = SpreadLimit(key = "rating", start = 100.0)
        val type = valid.copy(match = valid.match.copy(quality = MatchQuality(spread = listOf(limit, limit))))

        assertEquals(1, QueueTypeValidator.validate(type).size)
    }

    private fun withRules(rules: List<MatchRule>): QueueType =
        valid.copy(match = valid.match.copy(rules = rules))

    private fun withFilters(filters: List<MatchFilter>): QueueType =
        valid.copy(match = valid.match.copy(filters = filters))

}

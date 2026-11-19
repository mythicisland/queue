package net.mythicisland.queue.shared.type

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatchFilterTest {

    @Test
    fun `values only accepts one of the listed values`() {
        val filter = MatchFilter(key = "version", values = listOf("1.21.4", "1.21.5"))

        assertTrue(filter.accepts(mapOf("version" to "1.21.5")))
        assertFalse(filter.accepts(mapOf("version" to "1.20.1")))
    }

    @Test
    fun `number range includes its bounds`() {
        val filter = MatchFilter(key = "rating", min = 0.0, max = 1500.0)

        assertTrue(filter.accepts(mapOf("rating" to "1500")))
        assertFalse(filter.accepts(mapOf("rating" to "1500.5")))
    }

    @Test
    fun `number range rejects values that are not a number`() {
        assertFalse(MatchFilter(key = "rating", min = 0.0).accepts(mapOf("rating" to "high")))
    }

    @Test
    fun `a missing property fails every filter except an absent one`() {
        assertFalse(MatchFilter(key = "region", values = listOf("eu")).accepts(emptyMap()))
        assertFalse(MatchFilter(key = "rating", max = 1500.0).accepts(emptyMap()))
        assertTrue(MatchFilter(key = "banned", present = false).accepts(emptyMap()))
    }

    @Test
    fun `present false rejects a ticket that has the property`() {
        assertFalse(MatchFilter(key = "banned", present = false).accepts(mapOf("banned" to "true")))
    }

}

package net.mythicisland.queue.runtime.leader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class LeaderElectionTest {

    @Test
    fun `a free lease is acquired`() {
        assertEquals(LeaseAction.ACQUIRE, decide(null))
    }

    @Test
    fun `a lease held by another instance is followed`() {
        assertEquals(LeaseAction.FOLLOW, decide(lease("other")))
    }

    @Test
    fun `the leader renews its own lease`() {
        assertEquals(LeaseAction.RENEW, decide(lease("self"), leading = true))
    }

    @Test
    fun `the leader steps down when another instance took the lease`() {
        assertEquals(LeaseAction.FOLLOW, decide(lease("other"), leading = true))
    }

    @Test
    fun `the leader steps down instead of reacquiring a vanished lease`() {
        assertEquals(LeaseAction.FOLLOW, decide(null, leading = true))
    }

    @Test
    fun `a restarted instance adopts its own lease on startup`() {
        assertEquals(LeaseAction.RENEW, decide(lease("self"), adopting = true))
    }

    @Test
    fun `an instance never takes a lease with its own id after startup`() {
        assertEquals(LeaseAction.FOLLOW, decide(lease("self")))
    }

    @Test
    fun `a lease renewed recently is kept`() {
        assertFalse(LeaderElection.isExpiring(4.seconds, 10.seconds))
    }

    @Test
    fun `the leader steps down once half of the ttl passed without renewal`() {
        assertTrue(LeaderElection.isExpiring(5.seconds, 10.seconds))
    }

    private fun decide(lease: Lease?, leading: Boolean = false, adopting: Boolean = false): LeaseAction =
        LeaderElection.decide(lease, "self", leading, adopting)

    private fun lease(holder: String) = Lease(holder, "$holder:4564")

}

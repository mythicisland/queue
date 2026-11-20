package net.mythicisland.queue.runtime.leader

import io.nats.client.JetStreamApiException
import io.nats.client.KeyValue
import io.nats.client.api.KeyValueConfiguration
import io.nats.client.api.StorageType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mythicisland.queue.runtime.nats.NatsClient
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.time.toJavaDuration

class LeaderElection(private val kv: KeyValue, private val self: Lease, private val ttl: Duration) {

    private val logger = LoggerFactory.getLogger(LeaderElection::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val currentTerm = MutableStateFlow<Term?>(null)
    private val currentLeader = MutableStateFlow<Lease?>(null)
    private val lastRead = AtomicReference(TimeSource.Monotonic.markNow())
    private val currentlyHealthy = MutableStateFlow(true)
    private val failing = AtomicBoolean(false)

    val leader: StateFlow<Lease?> get() = currentLeader

    val terms: Flow<Long?> get() = currentTerm.map { it?.id }.distinctUntilChanged()

    val healthy: StateFlow<Boolean> get() = currentlyHealthy

    fun start() {
        scope.launch {
            refresh(adopting = true)
            while (isActive) {
                delay(ttl / 5)
                refresh(adopting = false)
            }
        }
    }

    suspend fun stop() {
        scope.coroutineContext.job.cancelAndJoin()
    }

    suspend fun release() = mutex.withLock {
        val term = currentTerm.value ?: return@withLock
        stepDown("Released")
        try {
            kv.delete(KEY, term.revision)
        } catch (e: Exception) {
            logger.warn("Failed to release leader lease", Field.error(e))
        }
    }

    private suspend fun refresh(adopting: Boolean) = mutex.withLock {
        try {
            val entry = kv.get(KEY)
            lastRead.set(TimeSource.Monotonic.markNow())
            val value = entry?.value
            val lease = if (value == null) null else Lease.decode(value)

            when (decide(lease, self.holder, currentTerm.value != null, adopting)) {
                LeaseAction.ACQUIRE -> acquire()
                LeaseAction.RENEW -> renew(currentTerm.value?.revision ?: entry?.revision ?: return@withLock)
                LeaseAction.FOLLOW -> follow(lease)
            }
            if (failing.compareAndSet(true, false)) {
                logger.info("Leader lease refresh recovered")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (failing.compareAndSet(false, true)) {
                logger.warn("Failed to refresh leader lease, retrying", Field.error(e))
            }
        }

        val sinceRead = lastRead.get().elapsedNow()
        if (isStale(sinceRead, ttl)) {
            forgetLeader()
        }
        updateHealth(sinceRead)

        val term = currentTerm.value ?: return@withLock
        if (isExpiring(term.renewedAt.elapsedNow(), ttl)) {
            stepDown("Leader lease could not be renewed in time")
        }
    }

    private fun forgetLeader() {
        val leader = currentLeader.value ?: return
        if (currentTerm.value != null) {
            return
        }
        currentLeader.value = null
        logger.warn("Lost track of the leader because the leader lease can't be read", Field("instanceId", leader.holder))
    }

    private fun updateHealth(sinceRead: Duration) {
        val healthy = !isUnhealthy(sinceRead)
        if (currentlyHealthy.value == healthy) {
            return
        }
        currentlyHealthy.value = healthy
        if (healthy) {
            logger.info("Leader election is healthy again")
        } else {
            logger.error("Leader election is unhealthy", Field("failingSeconds", sinceRead.inWholeSeconds))
        }
    }

    private fun acquire() {
        val start = TimeSource.Monotonic.markNow()
        try {
            lead(kv.create(KEY, self.encode()), start)
        } catch (_: JetStreamApiException) {
            return
        }
    }

    private fun renew(revision: Long) {
        val start = TimeSource.Monotonic.markNow()
        try {
            lead(kv.update(KEY, self.encode(), revision), start)
        } catch (_: JetStreamApiException) {
            stepDown("Leader lease was taken over")
        }
    }

    private fun lead(revision: Long, renewedAt: TimeMark) {
        val term = currentTerm.value
        if (term == null) {
            logger.info("Became leader", Field("instanceId", self.holder), Field("address", self.address))
            currentTerm.value = Term(revision, revision, renewedAt)
        } else {
            currentTerm.value = term.copy(revision = revision, renewedAt = renewedAt)
        }
        currentLeader.value = self
    }

    private fun follow(lease: Lease?) {
        stepDown("Leader lease is held by another instance")
        val leader = if (lease?.holder == self.holder) null else lease
        if (leader != null && leader != currentLeader.value) {
            logger.info("Following leader", Field("instanceId", leader.holder), Field("address", leader.address))
        }
        currentLeader.value = leader
    }

    private fun stepDown(reason: String) {
        if (currentTerm.value == null) {
            return
        }
        currentTerm.value = null
        currentLeader.value = null
        logger.warn("Lost leadership", Field("instanceId", self.holder), Field("reason", reason))
    }

    private data class Term(val id: Long, val revision: Long, val renewedAt: TimeMark)

    companion object {
        private const val KEY = "leader"
        private val UNHEALTHY_AFTER = 2.minutes

        fun init(nats: NatsClient, bucket: String, self: Lease, ttl: Duration): LeaderElection {
            val config = KeyValueConfiguration.builder()
                .name(bucket)
                .storageType(StorageType.Memory)
                .ttl(ttl.toJavaDuration())
                .build()
            return LeaderElection(nats.bucket(config), self, ttl)
        }

        fun decide(lease: Lease?, self: String, leading: Boolean, adopting: Boolean): LeaseAction = when {
            lease == null -> if (leading) LeaseAction.FOLLOW else LeaseAction.ACQUIRE
            lease.holder != self -> LeaseAction.FOLLOW
            leading || adopting -> LeaseAction.RENEW
            else -> LeaseAction.FOLLOW
        }

        fun isExpiring(sinceRenewal: Duration, ttl: Duration): Boolean = sinceRenewal >= ttl / 2

        fun isStale(sinceRead: Duration, ttl: Duration): Boolean = sinceRead >= ttl

        fun isUnhealthy(sinceRead: Duration): Boolean = sinceRead >= UNHEALTHY_AFTER

    }

}

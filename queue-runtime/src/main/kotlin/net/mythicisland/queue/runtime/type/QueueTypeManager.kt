package net.mythicisland.queue.runtime.type

import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mythicisland.queue.runtime.event.EventPublisher
import net.mythicisland.queue.runtime.server.ServerRegistry
import net.mythicisland.queue.runtime.ticket.TicketService
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import net.mythicisland.queue.shared.type.QueueType

class QueueTypeManager(
    private val types: QueueTypeRegistry,
    private val store: QueueTypeStore,
    private val servers: ServerRegistry,
    private val tickets: TicketService,
    private val events: EventPublisher,
) {

    private val logger = LoggerFactory.getLogger(QueueTypeManager::class)
    private val mutex = Mutex()

    suspend fun load() = mutex.withLock {
        for (type in store.load()) {
            val problems = QueueTypeValidator.validate(type)
            if (problems.isNotEmpty()) {
                logProblems(type.name, problems)
                continue
            }
            types.put(type)
        }
    }

    suspend fun sync(files: Collection<QueueType>) = mutex.withLock {
        val problems = files.flatMap { type -> QueueTypeValidator.validate(type).map { "${type.name}: $it" } } +
            QueueTypeValidator.validateGroups(files)
        if (problems.isNotEmpty()) {
            for (problem in problems) {
                logger.error("Invalid queue type file", Field("problem", problem))
            }
            throw IllegalStateException("Found ${problems.size} problems in the queue type files")
        }

        val changes = QueueTypeChanges.between(types.all(), files)
        for (type in changes.removed) {
            erase(type)
        }
        for (type in changes.changed) {
            write(type)
        }
        logger.info("Synced queue types from files", Field("changed", changes.changed.size), Field("removed", changes.removed.size))
    }

    suspend fun create(type: QueueType, validateOnly: Boolean): QueueType {
        mutex.withLock {
            if (types.get(type.name) != null) {
                throw Status.ALREADY_EXISTS.withDescription("Queue type ${type.name} already exists").asException()
            }
            check(type)
            if (validateOnly) {
                return type
            }
            write(type)
        }
        applied(null, type)
        return type
    }

    suspend fun update(type: QueueType, validateOnly: Boolean): QueueType {
        val previous = mutex.withLock {
            val current = types.get(type.name) ?: throw notFound(type.name)
            check(type)
            if (validateOnly) {
                return type
            }
            write(type)
            current
        }
        applied(previous, type)
        return type
    }

    suspend fun setActive(name: String, active: Boolean): QueueType {
        val previous = mutex.withLock {
            val current = types.get(name) ?: throw notFound(name)
            if (current.active == active) {
                return current
            }
            write(current.copy(active = active))
            current
        }
        val updated = previous.copy(active = active)
        applied(previous, updated)
        return updated
    }

    suspend fun delete(name: String): QueueType {
        val removed = mutex.withLock {
            val current = types.get(name) ?: throw notFound(name)
            erase(current)
            current
        }
        tickets.recover()
        return removed
    }

    suspend fun apply(type: QueueType): Boolean {
        val previous = mutex.withLock {
            val problems = problems(type)
            if (problems.isNotEmpty()) {
                logProblems(type.name, problems)
                return false
            }
            val current = types.get(type.name)
            if (current?.toProto() == type.toProto()) {
                return true
            }
            write(type)
            current
        }
        applied(previous, type)
        return true
    }

    suspend fun remove(name: String) {
        mutex.withLock {
            val current = types.get(name) ?: return
            erase(current)
        }
        tickets.recover()
    }

    private suspend fun applied(previous: QueueType?, current: QueueType) {
        val group = current.server.group
        if (previous?.server?.group != group && types.byGroup(group).all { it.name == current.name }) {
            servers.load(setOf(group))
        }
        if (previous != null) {
            tickets.recover()
        }
    }

    private fun write(type: QueueType) {
        try {
            store.put(type)
        } catch (e: Exception) {
            throw unavailable("Failed to store queue type ${type.name}", e)
        }

        val previous = types.put(type)
        if (previous == null) {
            events.queueTypeCreated(type)
            logger.info("Created queue type", Field("queueType", type.name), Field("active", type.active))
        } else {
            events.queueTypeUpdated(type, previous)
            logger.info("Updated queue type", Field("queueType", type.name), Field("active", type.active))
        }
    }

    private fun erase(type: QueueType) {
        try {
            store.delete(type.name)
        } catch (e: Exception) {
            throw unavailable("Failed to delete queue type ${type.name}", e)
        }

        types.remove(type.name)
        events.queueTypeDeleted(type)
        logger.info("Deleted queue type", Field("queueType", type.name))
    }

    private fun check(type: QueueType) {
        val problems = problems(type)
        if (problems.isNotEmpty()) {
            throw Status.INVALID_ARGUMENT.withDescription(problems.joinToString("; ")).asException()
        }
    }

    private fun problems(type: QueueType): List<String> =
        QueueTypeValidator.validate(type) +
            QueueTypeValidator.validateGroups(types.all().filter { it.name != type.name } + type)

    private fun logProblems(name: String, problems: List<String>) {
        for (problem in problems) {
            logger.error("Invalid queue type", Field("queueType", name), Field("problem", problem))
        }
    }

    private fun notFound(name: String): StatusException =
        Status.NOT_FOUND.withDescription("Queue type $name does not exist").asException()

    private fun unavailable(description: String, cause: Exception): StatusException =
        Status.UNAVAILABLE.withDescription(description).withCause(cause).asException()

}

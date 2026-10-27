package net.mythicisland.queue.runtime.type

import kotlinx.coroutines.flow.Flow
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import net.mythicisland.queue.shared.repository.DirectoryEvent
import net.mythicisland.queue.shared.repository.YamlDirectoryRepository
import net.mythicisland.queue.shared.type.QueueType
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class QueueTypeRegistry(path: Path) {

    private val logger = LoggerFactory.getLogger(QueueTypeRegistry::class)
    private val repository = YamlDirectoryRepository(path, QueueType::class)
    private val types = ConcurrentHashMap<String, QueueType>()

    init {
        val loaded = repository.load().map { (name, type) -> type.copy(name = name) }
        val problems = loaded.flatMap { problems(it) }
        if (problems.isNotEmpty()) {
            throw IllegalStateException("Found ${problems.size} problems in the queue types at $path")
        }

        for (type in loaded) {
            types[type.name] = type
        }
        logger.info("Loaded queue types", Field("path", path), Field("count", types.size))
    }

    fun get(name: String): QueueType? = types[name]

    fun all(): Collection<QueueType> = types.values

    fun byGroup(group: String): List<QueueType> = types.values.filter { it.server.group == group }

    fun groups(): Set<String> = types.values.map { it.server.group }.toSet()

    fun isShared(group: String): Boolean = byGroup(group).any { it.server.shared != null }

    fun capacity(group: String): Int? = byGroup(group).mapNotNull { it.server.shared?.capacity }.maxOrNull()

    fun watch(): Flow<DirectoryEvent<QueueType>> = repository.watch()

    fun put(type: QueueType): Boolean {
        if (problems(type).isNotEmpty()) {
            return false
        }
        types[type.name] = type
        return true
    }

    fun remove(name: String): QueueType? = types.remove(name)

    private fun problems(type: QueueType): List<String> {
        val problems = QueueTypeValidator.validate(type)
        for (problem in problems) {
            logger.error("Invalid queue type", Field("queueType", type.name), Field("problem", problem))
        }
        return problems
    }

}

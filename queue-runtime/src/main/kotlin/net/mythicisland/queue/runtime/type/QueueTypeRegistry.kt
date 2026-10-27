package net.mythicisland.queue.runtime.type

import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import net.mythicisland.queue.shared.repository.YamlDirectoryRepository
import net.mythicisland.queue.shared.type.QueueType
import java.nio.file.Path

class QueueTypeRegistry(path: Path) {

    private val logger = LoggerFactory.getLogger(QueueTypeRegistry::class)
    private val types: Map<String, QueueType> = load(path)

    fun get(name: String): QueueType? = types[name]

    fun all(): Collection<QueueType> = types.values

    fun byGroup(group: String): List<QueueType> = types.values.filter { it.server.group == group }

    fun groups(): Set<String> = types.values.map { it.server.group }.toSet()

    fun isShared(group: String): Boolean = byGroup(group).any { it.server.shared != null }

    fun capacity(group: String): Int? = byGroup(group).mapNotNull { it.server.shared?.capacity }.maxOrNull()

    private fun load(path: Path): Map<String, QueueType> {
        val types = YamlDirectoryRepository(path, QueueType::class).load()
            .mapValues { (name, type) -> type.copy(name = name) }

        val problems = types.values.flatMap { type -> QueueTypeValidator.validate(type).map { type.name to it } }
        for ((name, problem) in problems) {
            logger.error("Invalid queue type", Field("queueType", name), Field("problem", problem))
        }
        if (problems.isNotEmpty()) {
            throw IllegalStateException("Found ${problems.size} problems in the queue types at $path")
        }

        logger.info("Loaded queue types", Field("path", path), Field("count", types.size))
        return types
    }

}

package net.mythicisland.queue.shared.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.reflect.KClass

class YamlDirectoryRepository<T : Any>(
    private val directory: Path,
    private val type: KClass<T>,
) {

    fun load(): Map<String, T> {
        if (directory.notExists()) {
            directory.createDirectories()
            return emptyMap()
        }

        return files().mapValues { (_, file) -> load(file) }
    }

    fun watch(): Flow<DirectoryEvent<T>> = flow {
        directory.createDirectories()
        FileSystems.getDefault().newWatchService().use { watcher ->
            directory.register(
                watcher,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE,
            )
            val known = contents().toMutableMap()

            while (currentCoroutineContext().isActive) {
                val key = watcher.poll(1, TimeUnit.SECONDS) ?: continue
                key.pollEvents()
                key.reset()

                val current = contents()
                for ((name, content) in current) {
                    if (known[name] != content) {
                        known[name] = content
                        emit(parse(name))
                    }
                }
                for (name in known.keys - current.keys) {
                    known.remove(name)
                    emit(DirectoryEvent.Removed(name))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun files(): Map<String, Path> =
        directory.listDirectoryEntries("*.{yml,yaml}").associateBy { it.nameWithoutExtension }

    private fun contents(): Map<String, String> = files().mapValues { (_, file) -> file.readText() }

    private fun parse(name: String): DirectoryEvent<T> = try {
        val file = files()[name] ?: throw IllegalStateException("$name does not exist anymore")
        DirectoryEvent.Changed(name, load(file))
    } catch (e: Exception) {
        DirectoryEvent.Failed(name, e)
    }

    private fun load(file: Path): T {
        val loader = YamlConfigurationLoader.builder()
            .path(file)
            .defaultOptions { options ->
                options.serializers { serializers ->
                    serializers.register(Duration::class.java, DurationSerializer())
                    serializers.registerAnnotatedObjects(objectMapperFactory())
                }
            }
            .build()

        val node = loader.load()
        val value = node.get(type.java) ?: throw IllegalStateException("$file is empty")

        val known = loader.createNode()
        known.set(type.java, value)
        val unknown = keys(node) - keys(known)
        if (unknown.isNotEmpty()) {
            throw IllegalStateException("$file has unknown keys: ${unknown.joinToString()}")
        }
        return value
    }

    private fun keys(node: ConfigurationNode): Set<String> =
        node.childrenMap().values.flatMap { child -> keys(child) + child.path().joinToString(".") }.toSet()

}

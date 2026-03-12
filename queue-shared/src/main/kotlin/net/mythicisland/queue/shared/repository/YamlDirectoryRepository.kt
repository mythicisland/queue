package net.mythicisland.queue.shared.repository

import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.loader.ParsingException
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.lang.reflect.Type
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import kotlin.io.path.isDirectory

data class EntityWithFile<E>(
    val entity: E,
    val fileName: String,
    val relativePath: String,
    val namespaceId: String,
    val file: File,
)

abstract class YamlDirectoryRepository<E, I>(
    private val directory: Path,
    private val clazz: Class<E>,
    private val watcherEvents: WatcherEvents<E> = WatcherEvents.empty(),
) {

    private val logger = LogManager.getLogger(this::class.java)

    private val watchService = FileSystems.getDefault().newWatchService()
    private val loaders = mutableMapOf<File, YamlConfigurationLoader>()
    protected val entities = mutableMapOf<File, EntityWithFile<E>>()
    private val watchKeys = mutableSetOf<WatchKey>()

    abstract fun getFileName(identifier: I): String

    fun delete(element: E): Boolean {
        val entityWithFile = entities.values.find { it.entity == element } ?: return false
        return deleteFile(entityWithFile.file)
    }

    fun getAll(): List<E> {
        return entities.values.map { it.entity }
    }

    fun getAllWithFiles(): List<EntityWithFile<E>> {
        return entities.values.toList()
    }

    fun filterByFileName(fileName: String): List<E> {
        return entities.values
            .filter { it.fileName == fileName }
            .map { it.entity }
    }

    fun filterByFileNamePattern(pattern: String): List<E> {
        val regex = pattern.replace("*", ".*").toRegex()
        return entities.values
            .filter { regex.matches(it.fileName) }
            .map { it.entity }
    }

    fun filterByRelativePath(relativePath: String): List<E> {
        return entities.values
            .filter { it.relativePath == relativePath }
            .map { it.entity }
    }

    fun filterByNamespaceId(namespaceId: String): List<E> {
        return entities.values
            .filter { it.namespaceId == namespaceId }
            .map { it.entity }
    }

    fun filterByNamespaceIdPattern(pattern: String): List<E> {
        val regex = pattern.replace("*", ".*").toRegex()
        return entities.values
            .filter { regex.matches(it.namespaceId) }
            .map { it.entity }
    }

    fun findByFileName(fileName: String): E? {
        return entities.values
            .find { it.fileName == fileName }
            ?.entity
    }

    fun findByNamespaceId(namespaceId: String): E? {
        return entities.values
            .find { it.namespaceId == namespaceId }
            ?.entity
    }

    fun findByNamespaceIdAndFileName(namespaceId: String, fileName: String): E? {
        return entities.values
            .find { it.namespaceId == namespaceId && it.fileName == fileName }
            ?.entity
    }

    fun findByNamespaceIdAndFileName(text: String): E? {
        return entities.values
            .firstOrNull { it.namespaceId == text.split("/")[0] && it.fileName == text.split("/")[1] }?.entity
    }

    fun getAllNamespaces(): Set<String> {
        return entities.values.map { it.namespaceId }.toSet()
    }

    fun load(): List<E> {
        if (!directory.toFile().exists()) {
            directory.toFile().mkdirs()
        }

        registerWatcherRecursively()

        return loadFromDirectory(directory)
    }

    private fun loadFromDirectory(dir: Path): List<E> {
        val results = mutableListOf<E>()

        try {
            Files.walk(dir).use { paths ->
                paths.filter { !it.isDirectory() && it.toString().endsWith(".yml") }
                    .forEach { path ->
                        load(path.toFile())?.let { results.add(it) }
                    }
            }
        } catch (ex: Exception) {
            logger.error("Error walking directory tree: ${ex.message}")
        }

        logger.info("Loaded ${results.size} entities")

        return results
    }

    private fun load(file: File): E? {
        try {
            val loader = getOrCreateLoader(file)
            val node = loader.load(ConfigurationOptions.defaults())
            val entity = node.get(clazz) ?: return null

            val relativePath = directory.relativize(file.toPath()).toString()
            val namespaceId = extractNamespaceId(file.toPath())

            val entityWithFile = EntityWithFile<E>(
                entity = entity,
                fileName = file.nameWithoutExtension,
                relativePath = relativePath,
                namespaceId = namespaceId,
                file = file
            )

            entities[file] = entityWithFile
            return entity
        } catch (ex: ParsingException) {
            val existedBefore = entities.containsKey(file)
            if (existedBefore) {
                logger.error("Could not load file ${file.name}. Switching back to an older version.")
                return null
            }

            logger.error("Could not load file ${file.name}. Make sure it's correctly formatted!")
            return null
        }
    }

    private fun extractNamespaceId(filePath: Path): String {
        val relativePath = directory.relativize(filePath)
        val pathParts = relativePath.toString().split(File.separator)

        // Remove the filename from the path parts
        val directoryParts = pathParts.dropLast(1)

        if (directoryParts.isEmpty()) {
            return ""
        }

        val lastUniqueIndex = findLastUniqueDirectoryIndex(directoryParts)

        return if (lastUniqueIndex >= 0) {
            directoryParts.drop(lastUniqueIndex).joinToString("/")
        } else {
            directoryParts.joinToString("/")
        }
    }

    private fun findLastUniqueDirectoryIndex(pathParts: List<String>): Int {
        // Create a map to track the last occurrence of each directory name
        val lastOccurrences = mutableMapOf<String, Int>()

        pathParts.forEachIndexed { index, part ->
            lastOccurrences[part] = index
        }

        // Find directories that appear multiple times
        val duplicateDirectories = mutableSetOf<String>()
        pathParts.forEachIndexed { index, part ->
            if (lastOccurrences[part] != index) {
                duplicateDirectories.add(part)
            }
        }

        // If no duplicates, return -1 to use the full path
        if (duplicateDirectories.isEmpty()) {
            return -1
        }

        // Find the last occurrence of any duplicate directory
        var lastDuplicateIndex = -1
        pathParts.forEachIndexed { index, part ->
            if (part in duplicateDirectories) {
                lastDuplicateIndex = maxOf(lastDuplicateIndex, index)
            }
        }

        return lastDuplicateIndex
    }

    private fun deleteFile(file: File): Boolean {
        val deletedSuccessfully = file.delete()
        val removedSuccessfully = entities.remove(file) != null
        return deletedSuccessfully && removedSuccessfully
    }

    protected fun save(fileName: String, entity: E, subDirectory: String = "") {
        val targetDir = if (subDirectory.isNotEmpty()) {
            directory.resolve(subDirectory).also {
                it.toFile().mkdirs()
            }
        } else {
            directory
        }

        val file = targetDir.resolve("$fileName.yml").toFile()
        val loader = getOrCreateLoader(file)
        val node = loader.createNode(ConfigurationOptions.defaults().serializers {
            it.register(Enum::class.java, GenericEnumSerializer)
        })
        node.set(clazz, entity)
        loader.save(node)

        val relativePath = directory.relativize(file.toPath()).toString()
        val namespaceId = extractNamespaceId(file.toPath())

        val entityWithFile = EntityWithFile(
            entity = entity,
            fileName = fileName,
            relativePath = relativePath,
            namespaceId = namespaceId,
            file = file
        )

        entities[file] = entityWithFile
    }

    protected fun saveWithNamespace(fileName: String, entity: E, namespaceId: String) {
        val namespacePath = namespaceId.replace("/", File.separator)
        save(fileName, entity, namespacePath)
    }

    private fun getOrCreateLoader(file: File): YamlConfigurationLoader {
        return loaders.getOrPut(file) {
            YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .defaultOptions { options ->
                    options.serializers { builder ->
                        builder.registerAnnotatedObjects(objectMapperFactory())
                        builder.register(Enum::class.java, GenericEnumSerializer)
                    }
                }.build()
        }
    }

    private fun registerWatcherRecursively(): Job {
        // Register the root directory
        registerDirectoryWatcher(directory)

        // Register all existing subdirectories
        try {
            Files.walk(directory).use { paths ->
                paths.filter { it.isDirectory() && it != directory }
                    .forEach { registerDirectoryWatcher(it) }
            }
        } catch (ex: Exception) {
            logger.error("Error registering watchers for subdirectories: ${ex.message}")
        }

        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val key = watchService.take()
                for (event in key.pollEvents()) {
                    val path = event.context() as? Path ?: continue
                    val watchedDir = key.watchable() as? Path ?: continue
                    val resolvedPath = watchedDir.resolve(path)

                    val kind = event.kind()
                    logger.info("Detected change in $resolvedPath (${getChangeStatus(kind)})")

                    when (kind) {
                        StandardWatchEventKinds.ENTRY_CREATE -> {
                            if (resolvedPath.isDirectory()) {
                                // New directory created, register watcher for it
                                registerDirectoryWatcher(resolvedPath)
                            } else if (resolvedPath.toString().endsWith(".yml")) {
                                val entity = load(resolvedPath.toFile())
                                if (entity != null) {
                                    watcherEvents.onCreate(entity)
                                }
                            }
                        }

                        StandardWatchEventKinds.ENTRY_MODIFY -> {
                            if (!resolvedPath.isDirectory() && resolvedPath.toString().endsWith(".yml")) {
                                val entity = load(resolvedPath.toFile())
                                if (entity != null) {
                                    watcherEvents.onModify(entity)
                                }
                            }
                        }

                        StandardWatchEventKinds.ENTRY_DELETE -> {
                            val entityWithFile = entities[resolvedPath.toFile()]
                            if (entityWithFile != null) {
                                entities.remove(resolvedPath.toFile())
                                watcherEvents.onDelete(entityWithFile.entity)
                            }
                        }
                    }
                }
                key.reset()
            }
        }
    }

    private fun registerDirectoryWatcher(dir: Path) {
        try {
            val key = dir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
            )
            watchKeys.add(key)
            logger.debug("Registered watcher for directory: $dir")
        } catch (ex: Exception) {
            logger.error("Failed to register watcher for directory $dir: ${ex.message}")
        }
    }

    private fun getChangeStatus(kind: WatchEvent.Kind<*>): String {
        return when (kind) {
            StandardWatchEventKinds.ENTRY_CREATE -> "Created"
            StandardWatchEventKinds.ENTRY_DELETE -> "Deleted"
            StandardWatchEventKinds.ENTRY_MODIFY -> "Modified"
            else -> "Unknown"
        }
    }

    fun close() {
        watchKeys.forEach { it.cancel() }
        watchKeys.clear()
        try {
            watchService.close()
        } catch (ex: Exception) {
            logger.error("Error closing watch service: ${ex.message}")
        }
    }

    interface WatcherEvents<E> {
        fun onCreate(entity: E)
        fun onDelete(entity: E)
        fun onModify(entity: E)

        companion object {
            fun <E> empty(): WatcherEvents<E> = object : WatcherEvents<E> {
                override fun onCreate(entity: E) {}
                override fun onDelete(entity: E) {}
                override fun onModify(entity: E) {}
            }
        }
    }

    object GenericEnumSerializer : TypeSerializer<Enum<*>> {
        override fun deserialize(type: Type, node: ConfigurationNode): Enum<*> {
            val value = node.string ?: throw SerializationException("No value present in node")

            if (type !is Class<*> || !type.isEnum) {
                throw SerializationException("Type is not an enum class")
            }

            @Suppress("UNCHECKED_CAST")
            return try {
                java.lang.Enum.valueOf(type as Class<out Enum<*>>, value)
            } catch (e: IllegalArgumentException) {
                throw SerializationException("Invalid enum constant")
            }
        }

        override fun serialize(type: Type, obj: Enum<*>?, node: ConfigurationNode) {
            node.set(obj?.name)
        }
    }
}
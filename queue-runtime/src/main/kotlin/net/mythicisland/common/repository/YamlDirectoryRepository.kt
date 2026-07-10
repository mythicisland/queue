package net.mythicisland.common.repository

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

/**
 * Represents an entity.
 *
 * @param E The type of the entity.
 */
data class Entity<E>(
    val entity: E,
    val fileName: String,
    val relativePath: String,
    val namespaceId: String,
    val file: File,
)

/**
 * An abstract repository capable of managing, loading, and automatically watching
 * YAML configuration files mapped to specific Kotlin objects.
 *
 * @param E The entity type managed by this repository.
 * @param I The unique identifier type used to map filenames to specific entities.
 * @property directory The root directory where files are stored and watched.
 * @property clazz The Java class of the entity, required for Configurate serialization.
 * @property watcherEvents Contextual event callbacks for file creations, modifications, and deletions.
 */
abstract class YamlDirectoryRepository<E, I>(
    private val directory: Path,
    private val clazz: Class<E>,
    private val watcherEvents: WatcherEvents<E> = WatcherEvents.empty(),
) {

    private val logger = LogManager.getLogger(this::class.java)
    private val watchService = FileSystems.getDefault().newWatchService()
    private val loaders = mutableMapOf<File, YamlConfigurationLoader>()
    private val watchKeys = mutableSetOf<WatchKey>()

    protected val entities = mutableMapOf<File, Entity<E>>()

    /**
     * Maps an identifier to its corresponding expected filename.
     *
     * @param identifier The identifier to map.
     * @return The expected filename (with or without `.yml` extension).
     */
    abstract fun getFileName(identifier: I): String

    /**
     * Deletes the file associated with the given element and removes it from the cache.
     *
     * @param element The entity instance to delete.
     * @return `true` if the file was successfully deleted and removed from cache, `false` otherwise.
     */
    fun delete(element: E): Boolean {
        val entityWithFile = entities.values.find { it.entity == element } ?: return false
        return deleteFile(entityWithFile.file)
    }

    /**
     * Retrieves all currently loaded entities.
     *
     * @return A list containing all cached entities.
     */
    fun getAll(): List<E> {
        return entities.values.map { it.entity }
    }

    /**
     * Finds a single entity by its identifier using the [getFileName] mapping.
     *
     * @param identifier The identifier to look up.
     * @return The entity, or null if no matching file/cache exists.
     */
    fun find(identifier: I): E? {
        val fileName = getFileName(identifier).removeSuffix(".yml")
        return findByFileName(fileName)
    }

    /**
     * Retrieves all currently loaded entities wrapped with their file metadata.
     *
     * @return A list of [Entity] structures.
     */
    fun getAllWithFiles(): List<Entity<E>> {
        return entities.values.toList()
    }

    /**
     * Filters all cached entities by their filename.
     *
     * @param fileName The name of the file to filter by (without extension).
     * @return A list of entities matching the filename criteria.
     */
    fun filterByFileName(fileName: String): List<E> {
        return entities.values
            .filter { it.fileName == fileName }
            .map { it.entity }
    }

    /**
     * Finds a single cached entity matching the exact filename.
     *
     * @param fileName The name of the file to search for (without extension).
     * @return The entity instance, or null if not found.
     */
    fun findByFileName(fileName: String): E? {
        return entities.values
            .find { it.fileName == fileName }
            ?.entity
    }

    /**
     * Retrieves all distinct namespaces present among the currently loaded entities.
     *
     * @return A unique set of namespace identifier strings.
     */
    fun getAllNamespaces(): Set<String> {
        return entities.values.map { it.namespaceId }.toSet()
    }

    /**
     * Initializes the repository by creating the target directory if necessary,
     * registering filesystem listeners, and loading all existing YAML files.
     *
     * @return A list of all successfully loaded entities.
     */
    fun load(): List<E> {
        val dirFile = directory.toFile()
        if (!dirFile.exists()) {
            dirFile.mkdirs()
        }

        registerWatcherRecursively()
        return loadFromDirectory(directory)
    }

    /**
     * Walks the given directory tree to find and parse all `.yml` files.
     */
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

    /**
     * Parses a single YAML file, constructs its metadata, and caches the result.
     */
    private fun load(file: File): E? {
        try {
            val loader = getOrCreateLoader(file)
            val node = loader.load(ConfigurationOptions.defaults())
            val entity = node.get(clazz) ?: return null
            val relativePath = directory.relativize(file.toPath()).toString()
            val namespaceId = extractNamespaceId(file.toPath())
            val entityWithFile = Entity<E>(entity, file.nameWithoutExtension, relativePath, namespaceId, file)

            entities[file] = entityWithFile
            return entity
        } catch (_: ParsingException) {
            if (entities.containsKey(file)) {
                logger.error("Could not load file ${file.name}. Switching back to an older version.")
            } else {
                logger.error("Could not load file ${file.name}. Make sure it's correctly formatted!")
            }
            return null
        }
    }

    /**
     * Determines the namespace string derived from the subdirectories leading up to the file.
     */
    private fun extractNamespaceId(filePath: Path): String {
        val relativePath = directory.relativize(filePath)
        val pathParts = relativePath.toString().split(File.separator)
        val directoryParts = pathParts.dropLast(1)

        if (directoryParts.isEmpty()) return ""

        val lastUniqueIndex = findLastUniqueDirectoryIndex(directoryParts)
        return if (lastUniqueIndex >= 0) {
            directoryParts.drop(lastUniqueIndex).joinToString("/")
        } else {
            directoryParts.joinToString("/")
        }
    }

    /**
     * Helper to find duplicates within the sub-paths to avoid conflicts and isolate distinct namespaces.
     */
    private fun findLastUniqueDirectoryIndex(pathParts: List<String>): Int {
        val lastOccurrences = mutableMapOf<String, Int>()
        pathParts.forEachIndexed { index, part -> lastOccurrences[part] = index }

        val duplicateDirectories = pathParts.filterIndexed { index, part ->
            lastOccurrences[part] != index
        }.toSet()

        if (duplicateDirectories.isEmpty()) return -1

        var lastDuplicateIndex = -1
        pathParts.forEachIndexed { index, part ->
            if (part in duplicateDirectories) {
                lastDuplicateIndex = maxOf(lastDuplicateIndex, index)
            }
        }

        return lastDuplicateIndex
    }

    /**
     * Internal implementation to delete the file handle and purge it from memory.
     */
    private fun deleteFile(file: File): Boolean {
        val deletedSuccessfully = file.delete()
        val removedSuccessfully = entities.remove(file) != null
        return deletedSuccessfully && removedSuccessfully
    }

    /**
     * Saves an entity to a file under the root directory or a specific subdirectory path.
     *
     * @param fileName The target filename (without extension).
     * @param entity The entity instance to serialize.
     * @param subDirectory Optional relative sub-paths where the file should be placed.
     */
    protected fun save(fileName: String, entity: E, subDirectory: String = "") {
        val targetDir = if (subDirectory.isNotEmpty()) {
            directory.resolve(subDirectory).also { it.toFile().mkdirs() }
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

        val entityWithFile = Entity(
            entity = entity,
            fileName = fileName,
            relativePath = relativePath,
            namespaceId = namespaceId,
            file = file
        )

        entities[file] = entityWithFile
    }

    /**
     * Saves an entity using a pre-determined namespace ID to define its folder layout.
     *
     * @param fileName The target filename (without extension).
     * @param entity The entity instance to serialize.
     * @param namespaceId The namespace string (slashes will be replaced by platform folder separators).
     */
    protected fun saveWithNamespace(fileName: String, entity: E, namespaceId: String) {
        val namespacePath = namespaceId.replace("/", File.separator)
        save(fileName, entity, namespacePath)
    }

    /**
     * Caches and builds Configurate loaders.
     */
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

    /**
     * Recursively walks existing trees, maps listeners.
     */
    private fun registerWatcherRecursively(): Job {
        registerDirectoryWatcher(directory)

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
                                registerDirectoryWatcher(resolvedPath)
                            } else if (resolvedPath.toString().endsWith(".yml")) {
                                load(resolvedPath.toFile())?.let { watcherEvents.onCreate(it) }
                            }
                        }

                        StandardWatchEventKinds.ENTRY_MODIFY -> {
                            if (!resolvedPath.isDirectory() && resolvedPath.toString().endsWith(".yml")) {
                                load(resolvedPath.toFile())?.let { watcherEvents.onModify(it) }
                            }
                        }

                        StandardWatchEventKinds.ENTRY_DELETE -> {
                            val targetFile = resolvedPath.toFile()
                            entities[targetFile]?.let { entityWithFile ->
                                entities.remove(targetFile)
                                watcherEvents.onDelete(entityWithFile.entity)
                            }
                        }
                    }
                }
                key.reset()
            }
        }
    }

    /**
     * Registers a single folder path into the JDK WatchService tracker.
     */
    private fun registerDirectoryWatcher(dir: Path) {
        try {
            val key = dir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
            )
            watchKeys.add(key)
            logger.debug("Registered watcher for directory: {}", dir)
        } catch (ex: Exception) {
            logger.error("Failed to register watcher for directory $dir: ${ex.message}")
        }
    }

    /**
     * Converts a generic file event into a string.
     */
    private fun getChangeStatus(kind: WatchEvent.Kind<*>): String {
        return when (kind) {
            StandardWatchEventKinds.ENTRY_CREATE -> "Created"
            StandardWatchEventKinds.ENTRY_DELETE -> "Deleted"
            StandardWatchEventKinds.ENTRY_MODIFY -> "Modified"
            else -> "Unknown"
        }
    }

    /**
     * Clear WatchKeys and close the WatchService.
     */
    fun close() {
        watchKeys.forEach { it.cancel() }
        watchKeys.clear()
        try {
            watchService.close()
        } catch (ex: Exception) {
            logger.error("Error closing watch service: ${ex.message}")
        }
    }

    /**
     * Interface for file watch events.
     *
     * @param E The entity type.
     */
    interface WatcherEvents<E> {
        /** Invoked when a new entity file is detected on the filesystem. */
        fun onCreate(entity: E)
        /** Invoked when a tracked entity file is deleted from the filesystem. */
        fun onDelete(entity: E)
        /** Invoked when an existing entity file is modified on the filesystem. */
        fun onModify(entity: E)

        companion object {
            /**
             * Generates an empty implementation.
             */
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
            } catch (_: IllegalArgumentException) {
                throw SerializationException("Invalid enum constant")
            }
        }

        override fun serialize(type: Type, obj: Enum<*>?, node: ConfigurationNode) {
            node.set(obj?.name)
        }
    }
}
package net.mythicisland.queue.shared.repository

import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists
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

        return directory.listDirectoryEntries("*.yml").associate { it.nameWithoutExtension to load(it) }
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

        return loader.load().get(type.java) ?: throw IllegalStateException("$file is empty")
    }

}

package net.mythicisland.queue.runtime.type

import kotlinx.coroutines.*
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import net.mythicisland.queue.shared.repository.DirectoryEvent
import net.mythicisland.queue.shared.repository.YamlDirectoryRepository
import net.mythicisland.queue.shared.type.QueueType
import java.nio.file.Path

class QueueTypeWatcher(
    private val path: Path,
    private val manager: QueueTypeManager,
) {

    private val logger = LoggerFactory.getLogger(QueueTypeWatcher::class)
    private val repository = YamlDirectoryRepository(path, QueueType::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun sync() {
        val files = repository.load().map { (name, type) -> type.copy(name = name) }
        manager.sync(files)
        logger.info("Loaded queue type files", Field("path", path), Field("count", files.size))
    }

    fun start() {
        scope.launch {
            try {
                repository.watch().collect { event -> handle(event) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Stopped watching queue types", Field.error(e))
            }
        }
    }

    suspend fun shutdown() {
        scope.coroutineContext.job.cancelAndJoin()
    }

    private suspend fun handle(event: DirectoryEvent<QueueType>) {
        try {
            when (event) {
                is DirectoryEvent.Changed -> changed(event.value.copy(name = event.name))
                is DirectoryEvent.Removed -> manager.remove(event.name)
                is DirectoryEvent.Failed -> logger.error("Failed to read queue type", Field("queueType", event.name), Field.error(event.error))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to apply queue type change", Field("queueType", event.name), Field.error(e))
        }
    }

    private suspend fun changed(type: QueueType) {
        if (!manager.apply(type)) {
            logger.warn("Ignored invalid queue type change", Field("queueType", type.name))
        }
    }

}

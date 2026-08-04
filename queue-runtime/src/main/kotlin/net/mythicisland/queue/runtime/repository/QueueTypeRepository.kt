package net.mythicisland.queue.runtime.repository

import net.mythicisland.moonrise.common.repository.YamlDirectoryRepository
import net.mythicisland.queue.shared.queue.QueueType
import java.nio.file.Path

/**
 * Repository for storing queue types.
 *
 * @param path the directory to store queue types.
 */
class QueueTypeRepository(
    path: Path
) : YamlDirectoryRepository<QueueType, String>(
    path,
    QueueType::class.java
) {

    /**
     * Gets the file name a queue type is stored under.
     *
     * @param identifier the name of the queue type.
     * @return the file name of the queue type.
     */
    override fun getFileName(identifier: String): String {
        return "$identifier.yml"
    }

}

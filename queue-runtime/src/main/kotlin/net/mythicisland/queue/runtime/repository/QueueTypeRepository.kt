package net.mythicisland.queue.runtime.repository

import net.mythicisland.queue.shared.queue.QueueType
import net.mythicisland.moonrise.common.repository.YamlDirectoryRepository
import java.nio.file.Path

/**
 * Repository for store queue types.
 *
 * @param path the directory to store queue types.
 */
class QueueTypeRepository(
    path: Path
) : YamlDirectoryRepository<QueueType, String>(
    path,
    QueueType::class.java
) {

    override fun getFileName(identifier: String): String {
        return "$identifier.yml"
    }

}
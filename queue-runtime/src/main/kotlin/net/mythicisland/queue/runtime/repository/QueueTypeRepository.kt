package net.mythicisland.queue.runtime.repository

import net.mythicisland.queue.shared.queue.QueueType
import net.mythicisland.queue.shared.repository.YamlDirectoryRepository
import java.nio.file.Paths

object QueueTypeRepository : YamlDirectoryRepository<QueueType, String>(
    Paths.get("types"),
    QueueType::class.java
) {

    override fun getFileName(identifier: String): String {
        return "$identifier.yml"
    }

}
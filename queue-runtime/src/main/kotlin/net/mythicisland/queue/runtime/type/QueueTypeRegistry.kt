package net.mythicisland.queue.runtime.type

import net.mythicisland.queue.shared.type.QueueType
import java.util.concurrent.ConcurrentHashMap

class QueueTypeRegistry {

    private val types = ConcurrentHashMap<String, QueueType>()

    fun get(name: String): QueueType? = types[name]

    fun all(): Collection<QueueType> = types.values

    fun byGroup(group: String): List<QueueType> = types.values.filter { it.server.group == group }

    fun groups(): Set<String> = types.values.map { it.server.group }.toSet()

    fun isShared(group: String): Boolean = byGroup(group).any { it.server.shared != null }

    fun capacity(group: String): Int? = byGroup(group).mapNotNull { it.server.shared?.capacity }.maxOrNull()

    fun put(type: QueueType): QueueType? = types.put(type.name, type)

    fun remove(name: String): QueueType? = types.remove(name)

}

package net.mythicisland.queue.runtime.type

import net.mythicisland.queue.shared.type.QueueType

data class QueueTypeChanges(
    val changed: List<QueueType>,
    val removed: List<QueueType>,
) {

    fun isEmpty(): Boolean = changed.isEmpty() && removed.isEmpty()

    companion object {
        fun between(current: Collection<QueueType>, wanted: Collection<QueueType>): QueueTypeChanges {
            val known = current.associateBy { it.name }
            val wantedNames = wanted.map { it.name }.toSet()
            return QueueTypeChanges(
                changed = wanted.filter { type -> known[type.name]?.toProto() != type.toProto() },
                removed = current.filter { it.name !in wantedNames },
            )
        }
    }

}

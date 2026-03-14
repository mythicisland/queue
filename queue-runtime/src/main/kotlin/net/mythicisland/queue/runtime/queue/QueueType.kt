package net.mythicisland.queue.runtime.queue

data class QueueType(
    val name: String,
    val group: String,
    val maxCapacity: Long = -1L,
    val minCapacity: Long = -1L,
    val waitingCountdownMillis: Long,
    val countdownMillis: Long,
) {
    fun toDefinition() : build.buf.gen.mythicisland.queue.v1.QueueType {
        return build.buf.gen.mythicisland.queue.v1.QueueType.newBuilder()
            .setName(name)
            .setGroup(group)
            .setMaxCapacity(maxCapacity.toInt())
            .setMinCapacity(minCapacity.toInt())
            .setWaitingCountdownMillis(waitingCountdownMillis)
            .setCountdownMillis(countdownMillis)
            .build()

    }

    companion object {
        fun fromDefinition(definition: build.buf.gen.mythicisland.queue.v1.QueueType): QueueType {
            return QueueType(
                name = definition.name,
                group = definition.group,
                maxCapacity = definition.maxCapacity.toLong(),
                minCapacity = definition.minCapacity.toLong(),
                waitingCountdownMillis = definition.waitingCountdownMillis,
                countdownMillis = definition.countdownMillis
            )
        }
    }
}
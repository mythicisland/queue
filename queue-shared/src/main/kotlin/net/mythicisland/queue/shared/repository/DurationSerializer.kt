package net.mythicisland.queue.shared.repository

import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import java.time.Duration
import java.util.function.Predicate

class DurationSerializer : ScalarSerializer<Duration>(Duration::class.java) {

    private val pattern = Regex("""(\d+)(ms|s|m|h)""")

    override fun deserialize(type: Type, obj: Any): Duration {
        val match = pattern.matchEntire(obj.toString().trim())
            ?: throw SerializationException(type, "Invalid duration '$obj', expected e.g. 500ms, 10s, 5m or 1h")
        val (amount, unit) = match.destructured

        return when (unit) {
            "ms" -> Duration.ofMillis(amount.toLong())
            "s" -> Duration.ofSeconds(amount.toLong())
            "m" -> Duration.ofMinutes(amount.toLong())
            else -> Duration.ofHours(amount.toLong())
        }
    }

    override fun serialize(item: Duration, typeSupported: Predicate<Class<*>>): Any =
        if (item.toMillisPart() == 0) "${item.seconds}s" else "${item.toMillis()}ms"

}

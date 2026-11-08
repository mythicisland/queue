package net.mythicisland.queue.shared.repository

import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import java.util.function.Predicate

class EnumSerializer<T : Enum<T>>(private val enumType: Class<T>) : ScalarSerializer<T>(enumType) {

    override fun deserialize(type: Type, obj: Any): T {
        val value = obj.toString().trim()
        return enumType.enumConstants.firstOrNull { pretty(it) == value }
            ?: throw SerializationException(type, "Invalid value '$value', expected one of ${allowed()}")
    }

    override fun serialize(item: T, typeSupported: Predicate<Class<*>>): Any = pretty(item)

    private fun pretty(value: T): String = value.name.lowercase().replace('_', '-')

    private fun allowed(): String = enumType.enumConstants.joinToString { pretty(it) }

}

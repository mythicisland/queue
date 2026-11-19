package net.mythicisland.queue.shared.type

import build.buf.gen.mythicisland.queue.v1.matchFilter
import build.buf.gen.mythicisland.queue.v1.numberFilter
import build.buf.gen.mythicisland.queue.v1.presenceFilter
import build.buf.gen.mythicisland.queue.v1.valuesFilter
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import build.buf.gen.mythicisland.queue.v1.MatchFilter as MatchFilterProto
import build.buf.gen.mythicisland.queue.v1.NumberFilter as NumberFilterProto

@ConfigSerializable
data class MatchFilter(
    val key: String = "",
    val values: List<String>? = null,
    val min: Double? = null,
    val max: Double? = null,
    val present: Boolean? = null,
) {

    fun accepts(properties: Map<String, String>): Boolean {
        val value = properties[key]
        if (present != null) {
            return (value != null) == present
        }
        if (value == null) {
            return false
        }
        if (values != null) {
            return value in values
        }

        val number = value.toDoubleOrNull() ?: return false
        return number.isFinite() && (min == null || number >= min) && (max == null || number <= max)
    }

    fun describe(): String = when {
        present == true -> "$key to be set"
        present == false -> "$key not to be set"
        values != null -> "$key to be one of ${values.joinToString()}"
        min != null && max != null -> "$key to be a number from ${format(min)} to ${format(max)}"
        min != null -> "$key to be a number of at least ${format(min)}"
        max != null -> "$key to be a number of at most ${format(max)}"
        else -> "$key to be a number"
    }

    fun toProto(): MatchFilterProto = matchFilter {
        key = this@MatchFilter.key
        if (this@MatchFilter.present != null) {
            presence = presenceFilter { present = this@MatchFilter.present }
        } else if (this@MatchFilter.values != null) {
            values = valuesFilter { values += this@MatchFilter.values }
        } else {
            number = toNumberProto()
        }
    }

    private fun toNumberProto(): NumberFilterProto = numberFilter {
        if (this@MatchFilter.min != null) {
            min = this@MatchFilter.min
        }
        if (this@MatchFilter.max != null) {
            max = this@MatchFilter.max
        }
    }

    private fun format(number: Double): String =
        if (number % 1.0 == 0.0) number.toLong().toString() else number.toString()

    companion object {
        fun fromProto(proto: MatchFilterProto): MatchFilter = when (proto.conditionCase) {
            MatchFilterProto.ConditionCase.VALUES -> MatchFilter(key = proto.key, values = proto.values.valuesList.toList())
            MatchFilterProto.ConditionCase.NUMBER -> MatchFilter(
                key = proto.key,
                min = if (proto.number.hasMin()) proto.number.min else null,
                max = if (proto.number.hasMax()) proto.number.max else null,
            )
            MatchFilterProto.ConditionCase.PRESENCE -> MatchFilter(key = proto.key, present = proto.presence.present)
            else -> MatchFilter(key = proto.key)
        }
    }

}

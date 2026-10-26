package net.mythicisland.queue.shared.logger

class Field(val key: String, val value: Any?) {

    companion object {
        fun error(value: Throwable): Field = Field("error", value)
    }

}

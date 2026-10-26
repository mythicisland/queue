package net.mythicisland.queue.shared.logger

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.spi.ExtendedLogger

class Logger internal constructor(
    private val delegate: ExtendedLogger,
    private val fields: List<Field>,
) {

    fun debug(message: String, vararg fields: Field) = log(Level.DEBUG, message, fields)

    fun info(message: String, vararg fields: Field) = log(Level.INFO, message, fields)

    fun warn(message: String, vararg fields: Field) = log(Level.WARN, message, fields)

    fun error(message: String, vararg fields: Field) = log(Level.ERROR, message, fields)

    fun with(vararg fields: Field): Logger = Logger(delegate, this.fields + fields)

    private fun log(level: Level, message: String, fields: Array<out Field>) {
        if (!delegate.isEnabled(level)) {
            return
        }

        val all = this.fields + fields
        val throwable = all.firstNotNullOfOrNull { it.value as? Throwable }
        delegate.logMessage(FQCN, level, null, StructuredMessage(message, all), throwable)
    }

    private companion object {

        val FQCN: String = Logger::class.java.name

    }

}

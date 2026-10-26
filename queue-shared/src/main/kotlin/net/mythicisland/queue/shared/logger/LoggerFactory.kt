package net.mythicisland.queue.shared.logger

import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass

object LoggerFactory {

    fun getLogger(type: KClass<*>): Logger =
        Logger(LogManager.getContext(false).getLogger(type.java.name), emptyList())

}

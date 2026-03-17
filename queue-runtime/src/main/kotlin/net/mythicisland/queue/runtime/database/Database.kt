package net.mythicisland.queue.runtime.database

import org.apache.logging.log4j.LogManager
import org.jooq.impl.DSL
import org.jooq.DSLContext

class Database(
    val context: DSLContext
) {
    private val logger = LogManager.getLogger(Database::class.java)

    fun setup() {
        try {
            logger.info("Setting up database...")
            System.setProperty("org.jooq.no-logo", "true")
            System.setProperty("org.jooq.no-tips", "true")
            val setupInputStream = Database::class.java.getResourceAsStream("/schema.sql")
                ?: throw IllegalArgumentException("Database schema not found.")
            val setupCommands = setupInputStream.bufferedReader().use { it.readText() }.split(";")
            setupCommands.forEach {
                val trimmed = it.trim()
                if (trimmed.isNotEmpty())
                    context.execute(DSL.sql(trimmed))
            }

            logger.info("Successfully setup database")
        } catch (e: Exception) {
            logger.error("Failed to setup database", e)
            throw e
        }
    }
}
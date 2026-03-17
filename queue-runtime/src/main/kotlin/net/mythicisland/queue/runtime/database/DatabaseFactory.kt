package net.mythicisland.queue.runtime.database

import org.jooq.impl.DSL

object DatabaseFactory {
    fun createDatabase(databaseUrl: String): Database {
        System.setProperty("org.jooq.no-logo", "true")
        System.setProperty("org.jooq.no-tips", "true")
        System.setProperty("org.jooq.no-version-check", "true")
        val databaseContext = DSL.using(databaseUrl)
        return Database(databaseContext)
    }
}
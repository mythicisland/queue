package net.mythicisland.queue.shared.extension

import java.util.UUID

fun String.asUUID(): UUID {
    return try {
        UUID.fromString(this)
    } catch (_: IllegalArgumentException) {
        throw NoSuchElementException("No uuid provided")
    }
}
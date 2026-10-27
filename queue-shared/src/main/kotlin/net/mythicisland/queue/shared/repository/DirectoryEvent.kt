package net.mythicisland.queue.shared.repository

sealed interface DirectoryEvent<T> {

    val name: String

    data class Changed<T>(override val name: String, val value: T) : DirectoryEvent<T>

    data class Removed<T>(override val name: String) : DirectoryEvent<T>

    data class Failed<T>(override val name: String, val error: Exception) : DirectoryEvent<T>

}

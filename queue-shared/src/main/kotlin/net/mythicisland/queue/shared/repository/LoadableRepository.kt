package net.mythicisland.queue.shared.repository

interface LoadableRepository<I, E> : Repository<I, E> {

    fun load(): List<E>

}
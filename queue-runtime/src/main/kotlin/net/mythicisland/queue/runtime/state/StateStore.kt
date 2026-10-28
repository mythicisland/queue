package net.mythicisland.queue.runtime.state

interface StateStore {

    fun load(): List<ByteArray>

    fun put(key: String, value: ByteArray)

    fun delete(key: String)

    fun retryFailed()

}

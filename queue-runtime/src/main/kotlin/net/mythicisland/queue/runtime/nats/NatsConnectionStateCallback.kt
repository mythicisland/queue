package net.mythicisland.queue.runtime.nats

fun interface NatsConnectionStateCallback {
    fun onConnectionStateChanged(connected: Boolean)
}
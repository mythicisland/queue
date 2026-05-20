package net.mythicisland.common.nats

fun interface NatsConnectionStateCallback {
    fun onConnectionStateChanged(connected: Boolean)
}
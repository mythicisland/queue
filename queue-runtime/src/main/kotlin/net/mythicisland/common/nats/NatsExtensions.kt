package net.mythicisland.common.nats

/**
 * Creates a NATS connection manager.
 */
fun createNatsConnectionManager(
    natsUrl: String,
    natsUser: String,
    natsSecret: String,
    errorListener: NatsErrorListener = NatsErrorListener(),
    connectionHandler: NatsConnectionHandler = NatsConnectionHandler()
): NatsConnectionManager {
    return NatsConnectionManager(
        natsUrl,
        natsUser,
        natsSecret,
        errorListener,
        connectionHandler,
    )
}
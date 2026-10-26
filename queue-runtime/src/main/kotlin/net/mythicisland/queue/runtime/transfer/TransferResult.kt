package net.mythicisland.queue.runtime.transfer

import java.util.UUID

data class TransferResult(
    val transferred: List<UUID>,
    val missing: List<UUID>,
)

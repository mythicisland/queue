## API Usage

### Dependency

```kotlin
// build.gradle.kts
repositories {
    maven("https://repo.xxjanisxx.dev/releases")
}

dependencies {
    implementation("net.mythicisland.queue:queue-api:0.0.1-beta.1")
}
```

### Connecting

```kotlin
val api = QueueApi.create(
    QueueApiOptions.builder()
        .grpcHost("localhost")
        .grpcPort(4564)
        .natsUrl("nats://localhost:4222")
        .natsUser("your-user")
        .natsSecret("your-secret")
        .build()
)
```

### Enqueue a player

```kotlin
val playerId = player.uniqueId

// Single player
api.player().enqueue("dev", playerId).await()

// Multiple players (party queue)
api.player().enqueue("dev", listOf(player1Id, player2Id)).await()
```

### Dequeue a player

```kotlin
api.player().dequeue(playerId).await()
```

### Error handling with coroutines

```kotlin
try {
    api.player().enqueue("dev", playerId).await()
    player.sendMessage("You have been added to the queue.")
} catch (e: StatusRuntimeException) {
    // gRPC errors: NOT_FOUND (unknown queue type), FAILED_PRECONDITION (already queued), etc.
    player.sendMessage(e.status.description ?: "Failed to join queue.")
}
```

### Data API

```kotlin
// Get all available queue types (e.g. for tab completion or selection GUI)
val types = api.data().getAllQueueTypes().await()
types.queueTypesList.forEach { println(it.name) }

// Check which queue a player is in
val response = api.data().getQueueByPlayer(playerId).await()
val queue = response.queue
println("Player is in queue ${queue.type} (${queue.status})")

// Get player's position in their queue
val position = api.data().getPlayerPosition(playerId).await()
println("Position: ${position.position}/${position.queue.playerIdsCount}")

// Get all active queues of a specific type
val queues = api.data().getQueuesByType("dev").await()
println("${queues.queuesCount} active dev queues")

// Get a specific queue type's configuration
val type = api.data().getQueueType("dev").await()
println("${type.queueType.name}: ${type.queueType.minCapacity}-${type.queueType.maxCapacity} players")

// Get a specific queue by ID
val queue = api.data().getQueue(queueId).await()

// Get rating and activity stats for a specific queue type
val stats = api.data().getQueueTypeStats("bedwars").await()
println("${stats.stats.queueType}: ${stats.stats.rating} (${stats.stats.sharePercent}% share)")

// Get rating and activity stats for all queue types
val allStats = api.data().getAllQueueTypeStats().await()
allStats.statsList.forEach {
    println("${it.queueType}: ${it.rating} — ${it.totalPlayers24H} players today, ${it.trendPercent}% trend")
}
```

### Event API

Listen to real-time queue lifecycle events via NATS:

```kotlin
// Player events
api.event().player().onEnqueued { event ->
    println("${event.playerIds()} joined queue ${event.queueType()} (${event.queueId()})")
}

api.event().player().onDequeued { event ->
    println("${event.playerIds()} left queue ${event.queueType()}")
}

// Queue lifecycle events
api.event().queue().onCreated { event ->
    println("New queue created: ${event.queueId()} (type: ${event.queueType()})")
}

api.event().queue().onStatusUpdated { event ->
    println("Queue ${event.queueId()}: ${event.oldStatus()} -> ${event.newStatus()}")
}

api.event().queue().onServerAssigned { event ->
    println("Queue ${event.queueId()} assigned to server ${event.serverId()}")
}

api.event().queue().onTransfer { event ->
    println("${event.transferredPlayerIds().size} players transferred to ${event.serverId()}")
}

api.event().queue().onDeleted { event ->
    println("Queue ${event.queueId()} deleted")
}

// General queue change listener (fires on any state change)
api.event().queue().onUpdated { event ->
    println("Queue ${event.queueId()}: ${event.beforeStatus()} -> ${event.afterStatus()}")
}
```

Subscriptions can be cancelled:

```kotlin
val subscription = api.event().queue().onStatusUpdated { event ->
    // ...
}

// Later, when no longer needed
subscription.unsubscribe()
```

### Cleanup

```kotlin
api.close()
```
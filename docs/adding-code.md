# Adding Code

## Before You Start

**Check for existing types first.** Search packages for shared types before creating new ones.

**Placement rules:**
- `queue-runtime/.../service/` → Business logic (e.g., QueueService)
- `queue-runtime/.../repository/` → Data access (e.g., QueueRepository)
- `queue-runtime/.../extension/` → Kotlin extensions (e.g., PlayerExtension)
- `queue-runtime/.../visualizer/` → Player UI components
- `queue-proto/` → Protobuf definitions

**Quality rules:**
- All I/O operations **must** be async with coroutines (`suspend` + `.await()`)
- Never use `!!` unless you can prove it's impossible to be null
- Delete unused code - no backward compatibility exports
- Define behavior in ONE place (sealed classes, maps), not scattered if/switch
- If you find old patterns, refactor them to current standards

---

## Adding a Service

Services contain business logic and orchestrate repositories.

**1. Create the service class:**

```kotlin
// queue-runtime/src/main/kotlin/.../service/YourService.kt
package net.mythicisland.queue.runtime.your.service

import kotlinx.coroutines.future.await
import org.apache.logging.log4j.LogManager

/**
 * Handles [domain-specific functionality].
 */
class YourService(
    private val api: CloudApi,
    private val repository: YourRepository,
) {
    private val logger = LogManager.getLogger(YourService::class.java)

    /**
     * [Method description].
     *
     * @param param Description
     * @return Description
     */
    suspend fun doSomething(param: String): Result<Data> = runCatching {
        // Implementation
    }.onFailure { error ->
        logger.error("Failed to do something", error)
    }
}
```

**2. Register in dependency injection** (if applicable)

---

## Adding a Repository

Repositories handle data access and external API calls.

**1. Create the repository:**

```kotlin
// queue-runtime/src/main/kotlin/.../repository/YourRepository.kt
package net.mythicisland.queue.runtime.your.repository

import kotlinx.coroutines.future.await
import app.simplecloud.api.CloudApi

/**
 * Manages [data type] persistence and retrieval.
 */
class YourRepository(
    private val api: CloudApi,
) {
    private val cache = mutableMapOf<String, Data>()

    /**
     * Finds [entity] by [key].
     *
     * @param key The identifier
     * @return The entity, or null if not found
     */
    suspend fun find(key: String): Data? {
        return cache[key] ?: fetchFromApi(key)
    }

    private suspend fun fetchFromApi(key: String): Data? {
        return api.yourApi().getData(key).await()
    }
}
```

**Key patterns:**
- ✅ All API calls use `.await()`
- ✅ Use caching where appropriate
- ✅ Return nullable types for "not found" scenarios
- ✅ Use `Result<T>` for operations that can fail

---

## Adding an Extension Function

Extensions add utility methods to existing types.

**1. Create extension file:**

```kotlin
// queue-runtime/src/main/kotlin/.../extension/TypeExtension.kt
package net.mythicisland.queue.runtime.extension

import org.bukkit.entity.Player
import net.kyori.adventure.text.Component

/**
 * Sends a formatted queue status message to the player.
 */
fun Player.sendQueueStatus(position: Int, total: Int) {
    sendActionBar(
        Component.text("Queue Position: $position/$total")
            .color(NamedTextColor.GOLD)
    )
}

/**
 * Checks if the player is currently in a queue.
 */
suspend fun Player.isInQueue(): Boolean {
    // Implementation
    return false
}
```

**Naming convention:** `[Type]Extension.kt` (e.g., `PlayerExtension.kt`, `UUIDExtension.kt`)

---

## Adding a Visualizer

Visualizers handle player UI updates (actionbar, titles, messages).

**1. Create visualizer interface/implementation:**

```kotlin
// queue-runtime/src/main/kotlin/.../visualizer/YourVisualizer.kt
package net.mythicisland.queue.runtime.visualizer

import org.bukkit.entity.Player

/**
 * Visualizes [something] to players.
 */
interface YourVisualizer {
    /**
     * Displays [info] to the player.
     */
    fun display(player: Player, data: Data)
}

class YourVisualizerImpl : YourVisualizer {
    override fun display(player: Player, data: Data) {
        player.sendActionBar(/* ... */)
    }
}
```

**Key patterns:**
- Use Adventure API for all text components
- Support MiniMessage format for configurability
- Create tag resolvers for dynamic placeholders

---

## Adding a Protobuf Definition

**1. Define message in proto file:**

```protobuf
// queue-proto/mythicisland/queue/v1/your_types.proto
syntax = "proto3";

package mythicisland.queue.v1;

message YourMessage {
  string id = 1;
  int32 value = 2;
  repeated string tags = 3;
}
```

**2. Add service method if needed:**

```protobuf
// queue-proto/mythicisland/queue/v1/your_api.proto
service YourService {
  rpc GetData(GetDataRequest) returns (GetDataResponse);
}
```

**3. Generate code:**

```bash
./gradlew build  # Protobuf plugin auto-generates
```

**4. Use in Kotlin:**

```kotlin
val message = yourMessage {
    id = "123"
    value = 42
    tags += listOf("a", "b")
}
```

---

## Kotlin Best Practices

### Coroutines

```kotlin
// ❌ Bad - Blocking
val result = api.server().getServerById(id).get()

// ✅ Good - Async
val result = api.server().getServerById(id).await()
```

### Null Safety

```kotlin
// ❌ Bad - Can crash
val server = findServer(id)!!

// ✅ Good - Safe handling
val server = findServer(id) ?: return
// or
val server = requireNotNull(findServer(id)) { "Server not found: $id" }
```

### Error Handling

```kotlin
// ❌ Bad - Unhandled exceptions
suspend fun loadData(): Data {
    return api.getData().await()
}

// ✅ Good - Result type
suspend fun loadData(): Result<Data> = runCatching {
    api.getData().await()
}.onFailure { error ->
    logger.error("Failed to load data", error)
}
```

### Collections

```kotlin
// ❌ Bad - Multiple iterations
val result = servers
    .map { it.groupName }
    .filter { it.startsWith("lobby") }
    .map { it.uppercase() }

// ✅ Good - Single iteration with sequence
val result = servers
    .asSequence()
    .map { it.groupName }
    .filter { it.startsWith("lobby") }
    .map { it.uppercase() }
    .toList()
```

---

## Testing

**1. Create test file:**

```kotlin
// queue-runtime/src/test/kotlin/.../YourServiceTest.kt
package net.mythicisland.queue.runtime.your.service

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class YourServiceTest {
    @Test
    fun `should do something`() = runTest {
        // Arrange
        val service = YourService(mockApi, mockRepo)

        // Act
        val result = service.doSomething("input")

        // Assert
        assertEquals(expected, result)
    }
}
```

**Run tests:**

```bash
./gradlew test
./gradlew :queue-runtime:test  # Specific module
```

---

## Documentation

**Every public API needs KDoc:**

```kotlin
/**
 * Manages queue lifecycle from player join to server transfer.
 *
 * This service handles:
 * - Queue entry and exit
 * - Position tracking
 * - Server allocation
 * - Player transfer coordination
 *
 * @property api The SimpleCloud API instance
 * @property repository Queue data repository
 */
class QueueService(
    private val api: CloudApi,
    private val repository: QueueRepository,
) {
    /**
     * Adds a player to the queue for the specified type.
     *
     * @param player The player to enqueue
     * @param queueType The type of queue to join
     * @return Success with queue instance, or error
     * @throws IllegalArgumentException if queue type doesn't exist
     */
    suspend fun enqueue(player: Player, queueType: String): Result<Queue>
}
```

---

## Checklist Before Committing

- [ ] All new code is async with coroutines
- [ ] All public APIs have KDoc
- [ ] No `!!` operators (or justified in comment)
- [ ] Tests added for new functionality
- [ ] Error handling with `Result<T>` or try-catch
- [ ] Logging added for errors
- [ ] Code follows existing patterns
- [ ] No unused imports or dead code

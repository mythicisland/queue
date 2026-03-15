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

**Key patterns:**
- ✅ All API calls use `.await()`
- ✅ Use caching where appropriate
- ✅ Return nullable types for "not found" scenarios
- ✅ Use `Result<T>` for operations that can fail

---

## Adding an Extension Function

Extensions add utility methods to existing types.


**Naming convention:** `[Type]Extension.kt` (e.g., `PlayerExtension.kt`, `UUIDExtension.kt`)

---

**Key patterns:**
- Use Adventure API for all text components
- Support MiniMessage format for configurability
- Create tag resolvers for dynamic placeholders

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

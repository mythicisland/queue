# AGENTS.md

Queue management system for Minecraft server networks built as a SimpleCloud droplet.
Handles player queuing, server reservation, countdowns, and automatic transfers.
Built with Kotlin coroutines, gRPC, and NATS.

**Key context**: Players join queues via `/queue <type>` → reconciler manages lifecycle
(countdown → server reservation → game countdown → teleport) → players transferred to game server.

## Commands

```bash
./gradlew build                        # Build all modules
./gradlew test                         # Run all tests
./gradlew :queue-runtime:run           # Run the runtime
./gradlew :queue-api:publish           # Publish the API
```

## Architecture

| Module | Purpose |
|--------|---------|
| `queue-runtime/` | Standalone droplet: queue lifecycle, gRPC server, NATS events, database |
| `queue-api/` | Java/Kotlin client library (gRPC stubs, NATS failover, Kotlin coroutine extensions) |
| `queue-shared/` | Shared models, extensions, messages, events, and repository base classes |
| `queue-proto/` | Protobuf/gRPC service definitions and message types |

## Module Structure

```
queue/
├── queue-runtime/              # Standalone runtime (the droplet)
│   ├── launcher/               # CLI entry point (Clikt)
│   ├── database/               # Database connection + factory
│   ├── persistence/            # jOOQ-based persistence layer
│   ├── repository/             # QueueRepository, QueueTypeRepository
│   ├── reconciler/             # QueueStatusReconciler (lifecycle manager)
│   ├── service/                # gRPC endpoints (QueueService, QueueDataService)
│   ├── server/                 # ServerFinder (SimpleCloud discovery + reservation)
│   ├── visualizer/             # ActionbarVisualizer, tag resolvers
│   ├── event/                  # EventPublisher (NATS event publishing)
│   ├── nats/                   # NATS connection management + failover
│   └── QueueRuntime.kt         # Main runtime orchestrator
├── queue-shared/               # Shared across runtime and plugin
│   ├── queue/                  # Queue + QueueType data models
│   ├── message/                # Messages (MiniMessage templates) + PlayerMessenger
│   ├── event/                  # QueueEventNames (NATS subject constants)
│   ├── extension/              # PlayerExtension, UUIDExtension, MiniFontExtension
│   └── repository/             # Repository interfaces + YamlDirectoryRepository
├── queue-api/                  # Java/Kotlin client library
│   ├── api/                    # Public interfaces (QueueApi, QueuePlayerApi, etc.)
│   ├── api/event/              # Event API interfaces + event types
│   ├── api/internal/           # gRPC + NATS implementations
│   ├── builders/               # Kotlin DSL builder (QueueApiOptionsBuilder)
│   ├── extensions/             # Kotlin suspend extensions (enqueue/dequeue/data)
│   └── scopes/                 # Kotlin DSL scopes (EventScope, PlayerEventScope)
└── queue-proto/                # Protobuf definitions
    └── mythicisland/queue/v1/  # queue_api.proto, queue_events.proto, queue_types.proto
```

## File Placement

| Type | Location | Example |
|------|----------|---------|
| Data Models | `queue-shared/.../queue/` | `Queue.kt`, `QueueType.kt` |
| Messages | `queue-shared/.../message/` | `Messages.kt`, `PlayerMessenger.kt` |
| Extensions | `queue-shared/.../extension/` | `PlayerExtension.kt`, `UUIDExtension.kt` |
| Event Names | `queue-shared/.../event/` | `QueueEventNames.kt` |
| Repository Interfaces | `queue-shared/.../repository/` | `Repository.kt`, `YamlDirectoryRepository.kt` |
| Reconcilers | `queue-runtime/.../reconciler/` | `QueueStatusReconciler.kt` |
| Services | `queue-runtime/.../service/` | `QueueService.kt`, `QueueDataService.kt` |
| Repositories | `queue-runtime/.../repository/` | `QueueRepository.kt`, `QueueTypeRepository.kt` |
| Persistence | `queue-runtime/.../persistence/` | `PersistenceQueueRepository.kt` |
| Server Logic | `queue-runtime/.../server/` | `ServerFinder.kt` |
| Visualizers | `queue-runtime/.../visualizer/` | `ActionbarVisualizer.kt`, `QueueTagResolver.kt`, `ServerTagResolver.kt` |
| Event Publishing | `queue-runtime/.../event/` | `EventPublisher.kt` |
| NATS | `queue-runtime/.../nats/` | `NatsFailoverConnectionManager.kt` |
| Database | `queue-runtime/.../database/` | `Database.kt`, `DatabaseFactory.kt` |
| Launcher | `queue-runtime/.../launcher/` | `Launcher.kt`, `QueueStartCommand.kt` |
| Plugin Commands | `queue-plugin/.../command/` | `QueueCommandHandler.kt` |
| Plugin Listeners | `queue-plugin/.../listener/` | `NetworkQuitListener.kt` |
| Plugin Config | `queue-plugin/.../config/` | `QueueConfig.kt`, `YamlConfig.kt` |
| API Interfaces | `queue-api/.../api/` | `QueueApi.java`, `QueuePlayerApi.java` |
| API Internals | `queue-api/.../api/internal/` | `QueueApiImpl.java` |
| Protobuf | `queue-proto/.../v1/` | `queue_api.proto`, `queue_types.proto` |
| Tests | `*/src/test/kotlin/` | Mirror main package structure |

## Queue Lifecycle

```
NOT_ENOUGH_PLAYERS → WAITING_COUNTDOWN → SEARCHING_SERVER → WAITING_FOR_SERVER/SERVER_READY → COUNTDOWN → TELEPORTING → FINISHED
```

The `QueueStatusReconciler` is the heart of the system. It uses:
- **Delta-time countdown tracking** with `System.currentTimeMillis()` deltas
- **Per-queue Mutex** synchronization via `ConcurrentHashMap`
- **Per-type Mutex** on enqueue to prevent race conditions in queue creation
- **Do-while cascading** reconcile loop for immediate status transitions
- **500ms tick loops** for WAITING_COUNTDOWN and COUNTDOWN states
- **1s visualizer loop** for continuous actionbar sending
- **30s periodic reconciliation** as a safety net
- **SimpleCloud event subscriber** for server state changes (WAITING_FOR_SERVER)

## Key Implementation Details

- **Queue + QueueType models** live in `queue-shared`, used by both runtime and plugin
- **Queue type configs** are YAML files loaded by `QueueTypeRepository` (extends `YamlDirectoryRepository` from shared)
- **Countdown fields** in `QueueType` are in **seconds** (`waitingCountdownSeconds`, `countdownSeconds`), converted to millis in reconciler
- **Server names** in SimpleCloud follow `{groupName}-{numericalId}` format (e.g. `dev-1`). Use this for `player.connect()`, NOT `server.serverId` (which is a UUID)
- **`player.connect(serverName)`** returns `CompletableFuture<ConnectResult>` — must `.await()` and handle result
- **Actionbar** fades after ~2 seconds in Minecraft — requires continuous sending via dedicated loop
- **gRPC errors** map to: `NOT_FOUND` (unknown queue type), `FAILED_PRECONDITION` (already queued), `INTERNAL` (unexpected)
- **`queue-api`** is Java (not Kotlin) for broader compatibility — uses `CompletableFuture`, not coroutines
- **`queue-plugin`** is a Velocity plugin (not Spigot/Paper) — uses `SimpleCommand`, `ProxyServer`, Velocity's `@Plugin` annotation
- **Plugin uses a shared `CoroutineScope`** with `SupervisorJob() + Dispatchers.IO`, cancelled on shutdown

## Quality Philosophy

**The goal is always to leave the project better than it was—quality should always rise.**

- **Async-first**: All I/O operations use coroutines (`suspend` + `.await()`)
- **Type-safe**: Leverage Kotlin's type system; avoid `!!` operator
- **Clean code**: Check for existing types before creating new ones
- **No dead code**: Delete unused code immediately—no backward compatibility exports
- **Single source of truth**: Define behavior in ONE place (sealed classes, maps), not scattered if/switch statements
- **Continuous refactoring**: If you find old patterns while working, refactor them to current standards
- **Proper error handling**: Use `Result<T>` for expected failures, proper logging on errors

## Conventions

- [Adding Code](docs/adding-code.md) - How to add services, repositories, extensions, etc.

## Kotlin Standards

### Coroutines
- ✅ All API calls use `.await()`
- ✅ All suspend functions properly marked
- ✅ No `runBlocking` in production code
- ✅ Proper error handling with `try-catch` or `runCatching`

### Null Safety
- ✅ Use `?.` (safe call) and `?:` (elvis) over `!!`
- ✅ `requireNotNull()` with descriptive messages for validation
- ✅ Return nullable types for "not found" scenarios

### Documentation
- ✅ KDoc on all public classes and functions
- ✅ `@param`, `@return`, `@throws` documentation
- ✅ Code examples for complex APIs
- ✅ All docs in English

### Code Style
- ✅ Idiomatic Kotlin (scope functions, expression bodies, etc.)
- ✅ Data classes for DTOs
- ✅ Sealed classes for state hierarchies
- ✅ Extension functions over utility classes
- ✅ Immutable collections by default

## Workflow

### Planning
- Plan before building for non-trivial tasks (3+ steps or architectural decisions)
- Re-plan immediately if stuck or approach proves wrong
- Use `/plan` mode for complex features

### Problem Solving
- Fix bugs autonomously: find root cause, resolve completely
- No band-aids or temporary fixes
- If blocked, ask for clarification rather than guessing

### Feasibility First
- If a requested behavior is not fully possible, explicitly say so with constraints and tradeoffs BEFORE implementing
- Do not silently ship a "best effort" substitute without user confirmation
- Discuss alternatives and get alignment on approach

## Core Principles

- **Do It Right**: Write clean, well-structured code. If a refactor is needed to get there, do the refactor.
- **Be Thorough**: Follow through completely. Don't leave half-finished abstractions, dead code, or inconsistencies.
- **Professional Standards**: Find root causes. No shortcuts. Leave the codebase better than you found it.
- **Async Everything**: This is a Kotlin project—embrace coroutines fully.
- **Type Safety**: Use Kotlin's type system to prevent bugs at compile time.
- **Document Intent**: Code should be self-explanatory, but complex logic needs KDoc.

## Testing

```bash
./gradlew test                         # Run all tests
./gradlew :queue-runtime:test          # Run specific module tests
./gradlew test --info                  # Run with verbose output
./gradlew test --tests "QueueServiceTest"  # Run specific test class
```

Tests use:
- JUnit 5 for test framework
- `kotlinx-coroutines-test` for coroutine testing

---

**Questions?** Check [docs/adding-code.md](docs/adding-code.md) for detailed examples.

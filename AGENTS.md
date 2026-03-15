# AGENTS.md

Queue management system for Minecraft server networks. Handles player queuing, dynamic server provisioning,
and automatic transfers via SimpleCloud API integration. Built with Kotlin, coroutine, Using gRPC and NATS.

**Key context**: Players join queues → system allocates/provisions servers → handles transfers and status updates in real-time.

## Commands

```bash
./gradlew build                    # Build all modules
./gradlew test                     # Run all tests
./gradlew :queue-runtime:build     # Build specific module
./gradlew :queue-runtime:test      # Test specific module

# Protobuf
cd queue-proto && buf publish      # Publish to Buf registry
```

## Architecture

| Component | Location | Purpose |
|-----------|----------|---------|
| Runtime | `queue-runtime/` | Core queue logic, server management, reconciliation |
| Services | `queue-runtime/.../service/` | Business logic (QueueService) |
| Repositories | `queue-runtime/.../repository/` | Data access and caching |
| Server Finder | `queue-runtime/.../server/` | Server discovery and provisioning |
| Visualizers | `queue-runtime/.../visualizer/` | Player UI (actionbar, messages, titles) |
| Reconcilers | `queue-runtime/.../reconciler/` | State synchronization loops |
| Extensions | `queue-runtime/.../extension/` | Kotlin extension functions |
| Messages | `queue-runtime/.../message/` | Templated messages |
| Proto | `queue-proto/` | gRPC/Protobuf definitions |
| Plugin | `queue-plugin/` | Spigot/Paper plugin wrapper |
| API | `queue-api/` | Public API for other plugins |

## Module Structure

```
queue/
├── queue-runtime/     # Core implementation
│   ├── service/       # Business logic layer
│   ├── repository/    # Data access layer
│   ├── server/        # Server management
│   ├── visualizer/    # Player UI components
│   ├── reconciler/    # State reconciliation
│   └── extension/     # Kotlin extensions
├── queue-proto/       # Protobuf definitions
├── queue-plugin/      # Spigot plugin
├── queue-api/         # Public API
└── queue-shared/      # Shared types
```

## File Placement

| Type | Location | Example |
|------|----------|---------|
| Services | `queue-runtime/.../service/` | `QueueService.kt` |
| Repositories | `queue-runtime/.../repository/` | `QueueRepository.kt`, `QueueTypeRepository.kt` |
| Server Logic | `queue-runtime/.../server/` | `ServerFinder.kt` |
| Visualizers | `queue-runtime/.../visualizer/` | `ActionbarVisualizer.kt`, `QueueVisualizer.kt` |
| Reconcilers | `queue-runtime/.../reconciler/` | `QueueStatusReconciler.kt` |
| Extensions | `queue-runtime/.../extension/` | `PlayerExtension.kt`, `UUIDExtension.kt` |
| Messages | `queue-runtime/.../message/` | `Messages.kt` |
| Proto files | `queue-proto/mythicisland/queue/v1/` | `queue_types.proto`, `queue_api.proto` |
| Tests | `queue-runtime/src/test/kotlin/` | Mirror main package structure |

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
- [API Documentation](docs/docs.txt) - SimpleCloud API reference

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
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :queue-runtime:test

# Run with verbose output
./gradlew test --info

# Run specific test class
./gradlew test --tests "QueueServiceTest"
```

Tests use:
- JUnit 5 for test framework
- `kotlinx-coroutines-test` for coroutine testing

---

**Questions?** Check [docs/adding-code.md](docs/adding-code.md) for detailed examples.

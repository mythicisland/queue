plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(
    "queue-api",
    "queue-runtime",
    "queue-shared"
)

rootProject.name = "queue"
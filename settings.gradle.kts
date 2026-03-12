plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "queue"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs")
    }
}

include(
    "queue-api",
    "queue-plugin",
    "queue-runtime",
    "queue-shared"
)
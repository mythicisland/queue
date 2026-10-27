plugins {
    alias(libs.plugins.kraken)
    alias(libs.plugins.shadow)
}

dependencies {
    api(libs.queue.protos)
    api(libs.jnats)
    api(libs.bundles.grpc)
    implementation(project(":queue-shared"))
}

maven {
    url = "https://github.com/mythicisland/queue"
    description = "Kotlin API impl of Queue"

    repositories {
        register("releases") {
            url.set("https://repo.mythicisland.club/releases")
            username.set(System.getenv("MYTHICISLAND_USERNAME"))
            password.set(System.getenv("MYTHICISLAND_PASSWORD"))
        }
        register("snapshots") {
            url.set("https://repo.mythicisland.club/snapshots")
            username.set(System.getenv("MYTHICISLAND_USERNAME"))
            password.set(System.getenv("MYTHICISLAND_PASSWORD"))
        }
    }
}
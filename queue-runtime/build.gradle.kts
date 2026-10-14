plugins {
    alias(libs.plugins.jib)
    application
}

dependencies {
    implementation(project(":queue-shared"))
    implementation(libs.clikt)
    implementation(libs.jnats)
    implementation(libs.simplecloud.api)
    implementation(libs.bundles.grpc)
    implementation(libs.bundles.logging)
    implementation(libs.bundles.configurate)
}

application {
    mainClass.set("net.mythicisland.queue.runtime.launcher.LauncherKt")
}

jib {
    from {
        image = "eclipse-temurin:25-jre"
    }
    to {
        image = "ghcr.io/mythicisland/queue"
        tags = setOf(project.version.toString(), "latest")
    }
    container {
        mainClass = application.mainClass.get()
        jvmFlags = listOf("-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError")
        ports = listOf("4564")
        user = "1000:1000"
    }
}
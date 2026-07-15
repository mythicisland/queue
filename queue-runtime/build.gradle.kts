plugins {
    application
}

application {
    mainClass.set("net.mythicisland.queue.runtime.launcher.LauncherKt")
}

dependencies {
    implementation(project(":queue-shared"))
    implementation(libs.clikt)
    implementation(libs.jnats)
    implementation(libs.moonrise.common)
}
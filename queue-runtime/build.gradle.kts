plugins {
    application
}

application {
    mainClass.set("net.mythicisland.queue.runtime.launcher.LauncherKt")
}

dependencies {
    implementation(project(":queue-shared"))
    implementation(platform(libs.otel.bom))
    implementation(libs.bundles.otel)
    implementation(libs.clikt)
    implementation(libs.jnats)
}

tasks.named<Tar>("distTar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Zip>("distZip") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
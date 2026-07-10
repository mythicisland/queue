dependencies {
    api(libs.queue.proto)
    api(libs.bundles.grpc)
    api(libs.jnats)
}

tasks.shadowJar {
    mergeServiceFiles()

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    relocate("io.grpc", "net.mythicisland.queue.api.shaded.grpc")
    relocate("io.nats", "net.mythicisland.queue.api.shaded.nats")
    relocate("build.buf", "net.mythicisland.queue.api.shaded.buf")
}
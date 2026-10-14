plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    api(libs.jnats)
    api(libs.bundles.grpc)
    implementation(project(":queue-shared"))
}
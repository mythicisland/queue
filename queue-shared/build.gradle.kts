dependencies {
    api(libs.queue.protos)
    implementation(libs.simplecloud.api)
    implementation(libs.bundles.grpc)
    implementation(libs.bundles.logging)
    compileOnly(libs.bundles.configurate)
}

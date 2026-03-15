import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.run.velocity)
    kotlin("kapt")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    compileOnly(libs.velocity.api)
    kapt(libs.velocity.api)
    implementation(libs.bundles.configurate)
    implementation(project(":queue-api"))
}

tasks {
    runVelocity {
        velocityVersion("3.5.0-SNAPSHOT")
    }
}

tasks.named<ShadowJar>("shadowJar") {
    mergeServiceFiles()

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    relocate("io.grpc", "net.mythicisland.queue.plugin.shaded.grpc")
    relocate("org.spongepowered", "net.mythicisland.queue.plugin.shaded.configurate")
}
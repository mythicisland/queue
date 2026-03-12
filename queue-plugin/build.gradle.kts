import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.run.velocity)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
    implementation(project(":queue-shared"))
    implementation(project(":queue-api"))
}

tasks {
    runVelocity {
        velocityVersion("3.5.0-SNAPSHOT")
    }
}
plugins {
    kotlin("kapt")
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.velocity.api)
    kapt(libs.velocity.api)
    implementation(project(":queue-api"))
}

val generateBuildConstants by tasks.registering {
    val version = project.version.toString()
    val output = layout.buildDirectory.dir("generated/sources/buildConstants")
    inputs.property("version", version)
    outputs.dir(output)
    doLast {
        val file = output.get().file("net/mythicisland/queue/velocity/BuildConstants.kt").asFile
        file.parentFile.mkdirs()
        file.writeText("package net.mythicisland.queue.velocity\n\ninternal const val VERSION = \"$version\"\n")
    }
}

kotlin.sourceSets.main {
    kotlin.srcDir(generateBuildConstants)
}

tasks.shadowJar {
    dependencies {
        exclude(dependency("org.apache.logging.log4j:.*"))
        exclude(dependency("org.slf4j:.*"))
    }
}

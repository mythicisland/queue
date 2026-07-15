import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

allprojects {
    group = "net.mythicisland.queue"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://repo.simplecloud.app/snapshots")
        maven("https://buf.build/gen/maven")
        maven("https://repo.mythicisland.net/public")
    }
}

subprojects {
    apply {
        plugin("kotlin")
        plugin("com.gradleup.shadow")
    }

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        implementation(rootProject.libs.kotlinx.coroutines.core)
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            languageVersion = KotlinVersion.KOTLIN_2_4
            apiVersion = KotlinVersion.KOTLIN_2_4
        }
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.shadowJar {
        mergeServiceFiles()
        archiveFileName.set("${project.name}.jar")
    }
}

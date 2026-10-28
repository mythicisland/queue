import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

allprojects {
    group = "net.mythicisland.queue"
    version = "1.0.0-alpha.2"

    repositories {
        mavenCentral()
        maven("https://repo.simplecloud.app/snapshots")
        maven("https://repo.mythicisland.club/releases")
        maven("https://repo.mythicisland.club/snapshots")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://buf.build/gen/maven")
    }
}

subprojects {
    apply {
        plugin("kotlin")
    }

    configurations.configureEach {
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
        exclude(group = "build.buf", module = "protobuf-javalite")
    }

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        implementation(rootProject.libs.kotlinx.coroutines.core)
    }

    kotlin {
        jvmToolchain(25)
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
            languageVersion = KotlinVersion.KOTLIN_2_4
            apiVersion = KotlinVersion.KOTLIN_2_4
        }
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    tasks.test {
        useJUnitPlatform()
    }

}
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("maven-publish")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(libs.queue.proto)
    implementation(libs.bundles.grpc)
    implementation(libs.jnats)
}

tasks.named<ShadowJar>("shadowJar") {
    mergeServiceFiles()

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    relocate("io.grpc", "net.mythicisland.queue.api.shaded.grpc")
    relocate("io.nats", "net.mythicisland.queue.api.shaded.nats")
    relocate("build.buf", "net.mythicisland.queue.api.shaded.buf")
    archiveClassifier.set("")
}

tasks.named<Javadoc>("javadoc") {
    isFailOnError = false
    options {
        (this as StandardJavadocDocletOptions).apply {
            addStringOption("Xmaxerrs", "10000")
            addStringOption("Xmaxwarns", "10000")
        }
    }
}

val isSnapshot = version.toString().contains(Regex("beta|dev|alpha|snapshot", RegexOption.IGNORE_CASE))

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.named<ShadowJar>("shadowJar")) {
                classifier = ""
            }

            artifact(tasks.named<Jar>("javadocJar"))
            artifact(tasks.named<Jar>("sourcesJar"))

            pom {
                name.set("Queue API")
                description.set("Queue Java and Kotlin API.")
                url.set("https://github.com/mythicisland/queue")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("xxjanisxx")
                        name.set("Janis K.")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/mythicisland/queue.git")
                    developerConnection.set("scm:git:ssh://github.com/mythicisland/queue.git")
                    url.set("https://github.com/mythicisland/queue")
                }
            }
        }
    }

    repositories {
        maven {
            name = if (isSnapshot) "snapshots" else "releases"
            url = uri(
                if (isSnapshot) "https://repo.xxjanisxx.dev/private-snapshots"
                else "https://repo.xxjanisxx.dev/private-production"
            )
            credentials {
                username = findProperty("repoUser") as String? ?: System.getenv("REPO_USER")
                password = findProperty("repoPassword") as String? ?: System.getenv("REPO_PASSWORD")
            }
        }
    }
}
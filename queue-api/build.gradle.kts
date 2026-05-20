import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("maven-publish")
}

dependencies {
    api(libs.queue.proto)
    api(libs.bundles.grpc)
    api(libs.jnats)
}

tasks.named<ShadowJar>("shadowJar") {
    mergeServiceFiles()

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    relocate("io.grpc", "net.mythicisland.queue.api.shaded.grpc")
    relocate("io.nats", "net.mythicisland.queue.api.shaded.nats")
    relocate("build.buf", "net.mythicisland.queue.api.shaded.buf")
}

java {
    withJavadocJar()
    withSourcesJar()
}

val isSnapshot = version.toString().contains(Regex("dev|snapshot", RegexOption.IGNORE_CASE))

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
                description.set("API to interact with Queue")
                url.set("https://github.com/mythicisland/queue")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
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
                if (isSnapshot) "https://repo.xxjanisxx.dev/snapshots"
                else "https://repo.xxjanisxx.dev/releases"
            )
            credentials {
                username = findProperty("repoUser") as String? ?: System.getenv("REPO_USER")
                password = findProperty("repoPassword") as String? ?: System.getenv("REPO_PASSWORD")
            }
        }
    }
}
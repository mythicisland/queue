plugins {
    alias(libs.plugins.kraken)
}

dependencies {
    api(libs.queue.proto)
    api(libs.bundles.grpc)
    api(libs.jnats)
    implementation(libs.moonrise.common)
}

maven {
    artifactId = project.name
    url = "https://github.com/mythicisland/queue"
    description = "Java and Kotlin API to interact with Queue."

    organization {
        name = "Mythic Island"
        url = "https://github.com/mythicisland"
    }

    developers {
        developer {
            id = "xxjanisxx"
            name = "Janis"
            email = "xxjanisxx@proton.me"
        }
    }

    scm {
        url = "https://github.com/mythicisland/queue"
        connection = "scm:git:https://github.com/mythicisland/queue.git"
        developerConnection = "scm:git:ssh://git@github.com/mythicisland/queue.git"
    }

    ciManagement {
        system = "GitHub Actions"
        url = "https://github.com/mythicisland/queue/actions"
    }

    licenses {
        license {
            name = "The Apache Software License, Version 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
        }
    }

    repositories {
        register("public") {
            url.set("https://repo.mythicisland.net/public")
            username.set(System.getenv("MAVEN_USER"))
            password.set(System.getenv("MAVEN_TOKEN"))
        }
    }
}

tasks.shadowJar {
    mergeServiceFiles()

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    relocate("io.grpc", "net.mythicisland.queue.api.shaded.grpc")
    relocate("io.nats", "net.mythicisland.queue.api.shaded.nats")
    relocate("build.buf", "net.mythicisland.queue.api.shaded.buf")
}
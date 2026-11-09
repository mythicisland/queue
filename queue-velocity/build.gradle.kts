plugins {
    kotlin("kapt")
    alias(libs.plugins.shadow)
    alias(libs.plugins.blossom)
}

dependencies {
    compileOnly(libs.velocity.api)
    kapt(libs.velocity.api)
    implementation(project(":queue-api"))
}

sourceSets {
    main {
        blossom {
            kotlinSources {
                property("version", project.version.toString())
            }
        }
    }
}

tasks.shadowJar {
    archiveFileName = "queue-velocity.jar"
    dependencies {
        exclude(dependency("org.apache.logging.log4j:.*"))
        exclude(dependency("org.slf4j:.*"))
    }
}

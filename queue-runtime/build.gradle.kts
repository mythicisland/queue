plugins {
    alias(libs.plugins.jooq.codegen)
    application
}

application {
    mainClass.set("net.mythicisland.queue.runtime.launcher.LauncherKt")
}

dependencies {
    implementation(project(":queue-shared"))
    implementation(libs.clikt)
    implementation(libs.postgre.jdbc)
    implementation(libs.jnats)
    implementation(libs.bundles.jooq)

    jooqCodegen(libs.jooq.meta.extensions)
}

sourceSets {
    main {
        java { srcDirs("build/generated/source/db/main/java") }
        resources { srcDirs("src/main/db") }
    }
}

tasks.named("compileKotlin") {
    dependsOn(tasks.jooqCodegen)
}

tasks.named<Tar>("distTar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Zip>("distZip") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

jooq {
    configuration {
        generator {
            name = "org.jooq.codegen.KotlinGenerator"
            target {
                directory = "build/generated/source/db/main/java"
                packageName = "net.mythicisland.queue.db"
            }
            database {
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                properties {
                    property {
                        key = "scripts"
                        value = "src/main/db/schema.sql"
                    }
                    property {
                        key = "sort"
                        value = "semantic"
                    }
                    property {
                        key = "unqualifiedSchema"
                        value = "none"
                    }
                    property {
                        key = "defaultNameCase"
                        value = "lower"
                    }
                }
            }
        }
    }
}
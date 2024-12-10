import org.http4k.internal.ModuleLicense.Apache2
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "http4k Servirtium module"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("org.junit.jupiter:junit-jupiter-api:_")
    api(project(":http4k-platform-core"))
    api(project(":http4k-format-moshi"))
    api(project(":http4k-client-apache"))
    testImplementation("org.apiguardian:apiguardian-api:_")

    testImplementation(project(":http4k-format-jackson"))
    testImplementation(project(":http4k-testing-approval"))
    testImplementation(testFixtures(project(":http4k-core")))
}

tasks {
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }
}

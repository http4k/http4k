import org.http4k.internal.ModuleLicense.Apache2
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "http4k HTTP Server built on top of Ktor Netty engine"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-bridge-ktor"))
    api(libs.ktor.server.netty)
    testImplementation(testFixtures(project(":http4k-core")))
}

tasks {
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

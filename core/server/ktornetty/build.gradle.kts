import org.http4k.internal.ModuleLicense.Http4kCommercial
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "http4k HTTP Server built on top of Ktor Netty engine"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(Ktor.server.netty)
    testImplementation(testFixtures(project(":http4k-core")))
}

tasks {
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

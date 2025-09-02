import org.http4k.internal.ModuleLicense.Apache2
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "http4k Kotlinx Serialization JSON support"

plugins {
    id("org.http4k.community")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
}

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(libs.kotlinx.serialization.json)
    testImplementation(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(testFixtures(project(":http4k-api-openapi")))
    testImplementation(testFixtures(project(":http4k-api-jsonrpc")))
    testImplementation(project(":http4k-testing-approval"))
}

tasks {
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

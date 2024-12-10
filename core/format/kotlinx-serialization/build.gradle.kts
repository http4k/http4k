import org.http4k.internal.ModuleLicense.Apache2
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "http4k Kotlinx Serialization JSON support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(KotlinX.serialization.json)
    testImplementation(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(testFixtures(project(":http4k-contract")))
    testImplementation(testFixtures(project(":http4k-jsonrpc")))
    testImplementation(project(":http4k-testing-approval"))
}

tasks {
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

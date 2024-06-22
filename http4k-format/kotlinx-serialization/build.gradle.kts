import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "Http4k Kotlinx Serialization JSON support"

apply(plugin = "kotlinx-serialization")

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

description = "Http4k Kotlinx Serialization JSON support"

apply(plugin = "kotlinx-serialization")

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(KotlinX.serialization.json)
    testImplementation(project(":http4k-core"))
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-format-core", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-contract", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-jsonrpc", configuration = "testArtifacts"))
    testImplementation(project(":http4k-testing-approval"))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

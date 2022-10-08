description = "Http4k HTTP Server built on top of Ktor CIO engine"

dependencies {
    api(project(":http4k-core"))
    api(Ktor.server.cio)
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

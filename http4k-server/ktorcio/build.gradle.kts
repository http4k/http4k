description = "Http4k HTTP Server built on top of Ktor CIO engine"

dependencies {
    api(project(":http4k-core"))
    api("io.ktor:ktor-server-cio:_")
    testImplementation(testFixtures(project(":http4k-core")))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

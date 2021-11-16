description = "Http4k HTTP Server built on top of Ktor Netty engine"

dependencies {
    api(project(":http4k-core"))
    api("io.ktor:ktor-server-netty:_")
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "Http4k HTTP Server built on top of Ktor CIO engine"

dependencies {
    api(project(":http4k-core"))
    api(Ktor.server.cio)
    testImplementation(testFixtures(project(":http4k-core")))
}

tasks {
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

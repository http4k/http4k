description = "A set of kotest matchers for common http4k types"

dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-core"))
    api("io.kotest:kotest-assertions-core-jvm:_")
    testImplementation(project(":http4k-format-jackson"))
}

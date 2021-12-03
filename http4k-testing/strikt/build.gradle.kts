description = "A set of Strikt matchers for common http4k types"

dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-core"))
    api("io.strikt:strikt-core:_")
    testImplementation(project(":http4k-format-jackson"))
}

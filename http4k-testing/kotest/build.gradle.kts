description = "A set of kotest matchers for common http4k types"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
    id("org.http4k.code-coverage")
}

dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-core"))
    api("io.kotest:kotest-assertions-core-jvm:_")
    testImplementation(project(":http4k-format-jackson"))
}

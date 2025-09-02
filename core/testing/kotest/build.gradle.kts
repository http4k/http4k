

description = "A set of kotest matchers for common http4k types"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-core"))
    api(libs.kotest.assertions.core)
    testImplementation(project(":http4k-format-jackson"))
}

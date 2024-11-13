description = "A set of Strikt matchers for common http4k types"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
}

dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-core"))
    api(Testing.strikt.core)
    testImplementation(project(":http4k-format-jackson"))
}

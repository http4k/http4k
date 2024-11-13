description = "A set of Hamkrest matchers for common http4k types"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
    id("org.http4k.code-coverage")
}

dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-core"))
    api("com.natpryce:hamkrest:_")
    testImplementation(project(":http4k-format-jackson"))
}

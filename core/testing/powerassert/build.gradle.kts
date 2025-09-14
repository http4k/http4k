

description = "A set of Power Assert matchers for common http4k types"

plugins {
    id("org.http4k.community")
    alias(libs.plugins.powerAssert)
}

dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-core"))
    testImplementation(project(":http4k-format-jackson"))
}

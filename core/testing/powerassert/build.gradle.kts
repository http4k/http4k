import org.http4k.internal.ModuleLicense.Apache2

description = "A set of Power Assert matchers for common http4k types"

plugins {
    id("org.http4k.community")
    id("org.jetbrains.kotlin.plugin.power-assert") version "2.2.20-Beta2"
}

dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-core"))
    testImplementation(project(":http4k-format-jackson"))
}

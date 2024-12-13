import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "A set of kotest matchers for common http4k types"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-core"))
    api("io.kotest:kotest-assertions-core-jvm:_")
    testImplementation(project(":http4k-format-jackson"))
}

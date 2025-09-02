import org.http4k.internal.ModuleLicense.Apache2

description = "http4k JSON-RPC support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-core"))
    testImplementation(testFixtures(project(":http4k-core")))
}


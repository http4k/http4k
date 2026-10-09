

description = "http4k JSON-RPC support"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-core"))
    testImplementation(testFixtures(project(":http4k-core")))
}


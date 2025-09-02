

description = "http4k metrics support, integrating with micrometer.io"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-ops-core"))
    api(libs.micrometer.core)
    testImplementation(testFixtures(project(":http4k-core")))
}


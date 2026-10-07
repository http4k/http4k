

description = "http4k Failsafe support"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.failsafe)
    testImplementation(testFixtures(project(":http4k-core")))
}




description = "http4k Failsafe support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.failsafe)
    testImplementation(testFixtures(project(":http4k-core")))
}


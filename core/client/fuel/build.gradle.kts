

description = "HTTP Client built on top of fuel"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.fuel)
    testImplementation(testFixtures(project(":http4k-core")))
}

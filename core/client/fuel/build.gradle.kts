

description = "HTTP Client built on top of fuel"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.fuel)
    testImplementation(testFixtures(project(":http4k-core")))
}

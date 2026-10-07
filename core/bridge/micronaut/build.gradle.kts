

description = "http4k Bridge: from Micronaut to http4k"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-core"))

    implementation(libs.micronaut.http)

    testFixturesApi(testFixtures(project(":http4k-core")))
}

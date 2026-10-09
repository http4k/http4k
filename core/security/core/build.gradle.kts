

description = "http4k Security Core support"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
}

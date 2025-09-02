

description = "http4k Security Core support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
}

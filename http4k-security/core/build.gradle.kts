description = "Http4k Security Core support"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
}

dependencies {
    api(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
}

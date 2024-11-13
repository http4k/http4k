description = "Http4k Security Digest support"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-security-core"))
    testImplementation(testFixtures(project(":http4k-core")))
}

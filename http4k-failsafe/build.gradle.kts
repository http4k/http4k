description = "Http4k Failsafe support"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
    id("org.http4k.code-coverage")
}

dependencies {
    api(project(":http4k-core"))
    api("dev.failsafe:failsafe:_")
    testImplementation(testFixtures(project(":http4k-core")))
}

description = "Http4k templating core"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
    id("org.http4k.code-coverage")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    testImplementation(testFixtures(project(":http4k-core")))
}

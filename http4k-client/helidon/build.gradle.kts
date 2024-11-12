description = "HTTP Client built on top of Helidon"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
}

dependencies {
    api(project(":http4k-core"))
    api("io.helidon.webclient:helidon-webclient:_")
    testImplementation(testFixtures(project(":http4k-core")))
}

description = "HTTP Client built on top of Helidon"

plugins {
    id("org.http4k.apache-module")
}

dependencies {
    api(project(":http4k-core"))
    api("io.helidon.webclient:helidon-webclient:_")
    testImplementation(testFixtures(project(":http4k-core")))
}

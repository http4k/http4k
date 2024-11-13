description = "Http4k metrics support, integrating with micrometer.io"

plugins {
    id("org.http4k.apache-module")
}

dependencies {
    api(project(":http4k-core"))
    api("io.micrometer:micrometer-core:_")
    testImplementation(testFixtures(project(":http4k-core")))
}

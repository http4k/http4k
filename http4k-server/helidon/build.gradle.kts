description = "Http4k HTTP Server built on top of Helidon Nima"

plugins {
    id("org.http4k.module")
}

dependencies {
    api(project(":http4k-core"))
    api("io.helidon.webserver:helidon-webserver:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testRuntimeOnly("org.junit.platform:junit-platform-console-standalone:_")
}

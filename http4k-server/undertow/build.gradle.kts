description = "Http4k HTTP Server built on top of Undertow"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api("io.undertow:undertow-core:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))

    testImplementation(Testing.junit.jupiter.params)
}

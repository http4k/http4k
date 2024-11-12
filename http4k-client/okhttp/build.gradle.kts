description = "HTTP Client built on top of okhttp"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api(Square.okHttp3)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(path = ":http4k-server-undertow"))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

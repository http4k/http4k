description = "Http4k HTTP Server built on top of Netty"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api("io.netty:netty-codec-http2:_")
    testImplementation(testFixtures(project(":http4k-common")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

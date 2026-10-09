

description = "http4k HTTP Server built on top of Netty"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api(libs.netty.codec.http2)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

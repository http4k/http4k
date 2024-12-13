import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k HTTP Server built on top of Netty"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api("io.netty:netty-codec-http2:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

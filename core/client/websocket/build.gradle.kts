import org.http4k.internal.ModuleLicense.Apache2

description = "HTTP Websocket Client"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api(libs.java.websocket)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(path = ":http4k-server-jetty"))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

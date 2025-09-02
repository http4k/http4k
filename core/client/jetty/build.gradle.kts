import org.http4k.internal.ModuleLicense.Apache2

description = "HTTP Client built on top of jetty"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api(platform(libs.jetty.bom))
    api(libs.jetty.client)
    api(libs.jetty.websocket.jetty.client)

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-server-jetty"))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

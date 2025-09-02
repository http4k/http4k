

description = "HTTP Client built on top of Helidon"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api(platform(libs.helidon.bom))
    api(libs.helidon.webclient)
    api(libs.helidon.webclient.websocket)
    testImplementation(project(path = ":http4k-server-jetty")) // can use helidon when headers bug is fixed
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

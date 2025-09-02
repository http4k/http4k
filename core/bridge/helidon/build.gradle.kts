

description = "http4k Bridge: from Helidon to http4k"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))

    api(platform(libs.helidon.bom))
    api(libs.helidon.webserver)
    api(libs.helidon.webserver.sse)
    api(libs.helidon.webserver.websocket)

    testFixturesApi(testFixtures(project(":http4k-core")))
}

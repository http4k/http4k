import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bridge: from Helidon to http4k"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))

    api("io.helidon.webserver:helidon-webserver:_")
    api("io.helidon.webserver:helidon-webserver-sse:_")
    api("io.helidon.webserver:helidon-webserver-websocket:_")

    testFixturesApi(testFixtures(project(":http4k-core")))
}

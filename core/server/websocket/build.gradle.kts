import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k WS(S) Server implemented by TooTallNate/Java-WebSocket"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api("org.java-websocket:Java-WebSocket:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

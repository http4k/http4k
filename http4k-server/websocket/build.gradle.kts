description = "Http4k WS(S) Server implemented by TooTallNate/Java-WebSocket"

plugins {
    id("org.http4k.module")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api("org.java-websocket:Java-WebSocket:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

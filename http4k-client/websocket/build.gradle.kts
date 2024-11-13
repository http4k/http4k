description = "HTTP Websocket Client"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
    id("org.http4k.code-coverage")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api("org.java-websocket:Java-WebSocket:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(path = ":http4k-server-jetty"))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

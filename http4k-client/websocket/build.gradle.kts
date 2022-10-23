description = "HTTP Websocket Client"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api("org.java-websocket:Java-WebSocket:_")
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
    testImplementation(project(path = ":http4k-server-undertow"))
    testImplementation(project(path = ":http4k-realtime-core", configuration ="testArtifacts"))
}

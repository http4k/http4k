description = "HTTP Client built on top of jetty"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api(platform("org.eclipse.jetty:jetty-bom:_"))
    api("org.eclipse.jetty:jetty-client")
    api("org.eclipse.jetty.websocket:websocket-jetty-client")

    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-server-jetty"))
    testImplementation(project(path = ":http4k-realtime-core", configuration = "testArtifacts"))
}

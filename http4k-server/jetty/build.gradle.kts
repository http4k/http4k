description = "Http4k HTTP Server built on top of jetty"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api("org.eclipse.jetty:jetty-server:_")
    api("jakarta.servlet:jakarta.servlet-api:_")
    api("org.eclipse.jetty:jetty-servlet:_")

    api("org.eclipse.jetty.websocket:websocket-core-server:_")

    // this list is for reference since http2 support is optional
    implementation("org.eclipse.jetty.http2:http2-server:_")
    implementation("org.eclipse.jetty:jetty-alpn-java-server:_")
    implementation("org.mortbay.jetty.alpn:alpn-boot:_")

    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
    testImplementation(project(path = ":http4k-realtime-core", configuration ="testArtifacts"))
}

description = "Http4k HTTP Server built on top of jetty"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))

    api(platform("org.eclipse.jetty:jetty-bom:_"))

    api("org.eclipse.jetty:jetty-server")
    api("org.eclipse.jetty:jetty-servlet")
    api("jakarta.servlet:jakarta.servlet-api:_")

    api("org.eclipse.jetty.websocket:websocket-core-server")

    // this list is for reference since http2 support is optional
    implementation("org.eclipse.jetty.http2:http2-server")
    implementation("org.eclipse.jetty:jetty-alpn-java-server")
    implementation("org.mortbay.jetty.alpn:alpn-boot:_")

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

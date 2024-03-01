description = "Http4k HTTP Server built on top of jetty v11"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))

    val jettyVersion = "11.0.20"
    api(platform("org.eclipse.jetty:jetty-bom:$jettyVersion"))

    api("org.eclipse.jetty:jetty-server:$jettyVersion")
    api("org.eclipse.jetty:jetty-servlet:$jettyVersion")
    api("jakarta.servlet:jakarta.servlet-api:_")

    api("org.eclipse.jetty.websocket:websocket-core-server:$jettyVersion")

    // this list is for reference since http2 support is optional
    implementation("org.eclipse.jetty.http2:http2-server:$jettyVersion")
    implementation("org.eclipse.jetty:jetty-alpn-java-server:$jettyVersion")
    implementation("org.mortbay.jetty.alpn:alpn-boot:_")

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

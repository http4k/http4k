description = "HTTP Client built on top of jetty"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
    id("org.http4k.code-coverage")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api(platform("org.eclipse.jetty:jetty-bom:_"))
    api("org.eclipse.jetty:jetty-client")
    api("org.eclipse.jetty.websocket:jetty-websocket-jetty-client")

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(path = ":http4k-server-jetty"))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

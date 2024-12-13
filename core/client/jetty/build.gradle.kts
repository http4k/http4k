import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "HTTP Client built on top of jetty"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api(platform("org.eclipse.jetty:jetty-bom:_"))
    api("org.eclipse.jetty:jetty-client")
    api("org.eclipse.jetty.websocket:jetty-websocket-jetty-client")

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-server-jetty"))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

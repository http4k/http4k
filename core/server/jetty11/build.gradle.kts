import org.http4k.internal.ModuleLicense.Apache2

description = "http4k HTTP Server built on top of jetty v11"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api(project(":http4k-bridge-servlet"))

    api(platform(libs.jetty11.bom))
    api(libs.jetty11.server)
    api(libs.jetty11.servlet)
    api(libs.jakarta.servlet.api)

    api(libs.jetty11.websocket.core.server)

    // this list is for reference since http2 support is optional
    implementation(libs.jetty11.http2.server)
    implementation(libs.jetty11.alpn.java.server)
    implementation(libs.alpn.boot)

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
    testImplementation(testFixtures(project(":http4k-bridge-servlet")))
}

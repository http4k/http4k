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

    val jettyVersion = "11.0.20"
    api(platform("org.eclipse.jetty:jetty-bom:$jettyVersion"))

    api("org.eclipse.jetty:jetty-server:$jettyVersion")
    api("org.eclipse.jetty:jetty-servlet:$jettyVersion")
    api(libs.jakarta.servlet.api)

    api("org.eclipse.jetty.websocket:websocket-core-server:$jettyVersion")

    // this list is for reference since http2 support is optional
    implementation("org.eclipse.jetty.http2:http2-server:$jettyVersion")
    implementation("org.eclipse.jetty:jetty-alpn-java-server:$jettyVersion")
    implementation(libs.alpn.boot)

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
    testImplementation(testFixtures(project(":http4k-bridge-servlet")))
}

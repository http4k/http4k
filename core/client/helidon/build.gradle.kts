import org.http4k.internal.ModuleLicense.Apache2

description = "HTTP Client built on top of Helidon"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api(platform(libs.helidon.bom))
    api("io.helidon.webclient:helidon-webclient")
    api("io.helidon.webclient:helidon-webclient-websocket")
    testImplementation(project(path = ":http4k-server-jetty")) // can use helidon when headers bug is fixed
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
}

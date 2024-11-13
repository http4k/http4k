import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k HTTP Server built on top of Helidon Nima"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}


dependencies {
    api(project(":http4k-core"))
    api("io.helidon.webserver:helidon-webserver:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testRuntimeOnly("org.junit.platform:junit-platform-console-standalone:_")
}

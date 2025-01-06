import org.http4k.internal.ModuleLicense.Apache2

description = "http4k HTTP Server built on top of Helidon Nima"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-bridge-helidon"))


    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
    testRuntimeOnly("org.junit.platform:junit-platform-console-standalone:_")
}

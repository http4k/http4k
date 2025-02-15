import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k HTTP Server built on top of Undertow"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api("io.undertow:undertow-core:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-realtime-core")))
    testImplementation("org.junit.jupiter:junit-jupiter-params")
}

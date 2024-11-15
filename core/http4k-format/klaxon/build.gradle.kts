import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k Klaxon JSON support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api("com.beust:klaxon:_")
    testImplementation(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(project(":http4k-testing-approval"))
}

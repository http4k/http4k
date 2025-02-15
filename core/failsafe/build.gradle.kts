import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "DEPRECATED: Use http4k-ops-failsafe"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("dev.failsafe:failsafe:_")
    testImplementation(testFixtures(project(":http4k-core")))
}

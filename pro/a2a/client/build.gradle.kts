import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k A2A Client support"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-a2a-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-a2a-sdk"))
    testImplementation(project(":http4k-server-helidon"))
    testImplementation(project(":http4k-testing-approval"))

    // TODO remove?
    testImplementation(project(":http4k-security-core"))
}

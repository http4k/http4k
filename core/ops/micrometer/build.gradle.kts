import org.http4k.internal.ModuleLicense.Apache2

description = "http4k metrics support, integrating with micrometer.io"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-ops-core"))
    api(libs.micrometer.core)
    testImplementation(testFixtures(project(":http4k-core")))
}


import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k metrics support, integrating with micrometer.io"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("io.micrometer:micrometer-core:_")
    testImplementation(testFixtures(project(":http4k-core")))
}

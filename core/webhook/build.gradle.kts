import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Standard webhooks support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("dev.forkhandles:values4k:_")
    api(project(":http4k-format-core"))

    testImplementation(project(":http4k-format-jackson"))
    testImplementation(testFixtures(project(":http4k-core")))
}

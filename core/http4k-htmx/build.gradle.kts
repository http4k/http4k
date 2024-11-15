import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k htmx support utilities"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}

dependencies {
    api(project(":http4k-core"))
    api("org.webjars.npm:htmx.org:_")
    api("org.webjars.npm:hyperscript.org:_")
    api("dev.forkhandles:values4k:_")

    testImplementation(testFixtures(project(":http4k-core")))
}

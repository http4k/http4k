import org.http4k.internal.ModuleLicense.Apache2

description = "DEPRECATED: Use http4k-web-htmx"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("org.webjars.npm:htmx.org:_")
    api("org.webjars.npm:hyperscript.org:_")
    api("dev.forkhandles:values4k:_")

    // this is here to force the version of something which is no longer in maven
    implementation("org.webjars.npm:jridgewell__sourcemap-codec:1.5.0")

    testImplementation(testFixtures(project(":http4k-core")))
}

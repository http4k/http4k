import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k Datastar support utilities"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    implementation(project(":http4k-format-core"))
    implementation(project(":http4k-template-core"))

    api("dev.forkhandles:values4k:_")

    testImplementation(testFixtures(project(":http4k-core")))
}

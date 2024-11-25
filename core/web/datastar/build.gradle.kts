import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k Datastar support utilities"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.conventions")
//    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api("dev.forkhandles:values4k:_")

    testImplementation(testFixtures(project(":http4k-core")))
}

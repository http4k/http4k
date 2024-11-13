import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k Failsafe support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}


dependencies {
    api(project(":http4k-core"))
    api("dev.failsafe:failsafe:_")
    testImplementation(testFixtures(project(":http4k-core")))
}

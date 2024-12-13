import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k Security OAuth support"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-security-core"))
    api(project(":http4k-format-moshi")) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }
    api("dev.forkhandles:result4k:_")
    testImplementation(project(":http4k-format-jackson"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation("commons-codec:commons-codec:_")
}

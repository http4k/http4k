import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k Security: HTTP Message Signatures support"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-security-core"))
    api("dev.forkhandles:result4k:_")

    testImplementation(project(":http4k-testing-hamkrest"))
}

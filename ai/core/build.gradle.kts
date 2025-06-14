import org.http4k.internal.ModuleLicense.Apache2

description = "http4k AI core types"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
}

dependencies {
    api(platform("dev.forkhandles:forkhandles-bom:_"))
    api(project(":http4k-connect-core"))
    api("dev.forkhandles:result4k")

    api(project(":http4k-format-core"))

    api(project(":http4k-format-moshi")) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }
}

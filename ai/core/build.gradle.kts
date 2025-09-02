

description = "http4k AI core types"

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
}

dependencies {
    api(platform(libs.forkhandles.bom))
    api(project(":http4k-connect-core"))
    api(libs.result4k)

    api(project(":http4k-format-core"))

    api(project(":http4k-format-moshi")) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }
}

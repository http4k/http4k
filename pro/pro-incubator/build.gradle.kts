import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k Pro Incubator code"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.pro")
    id("org.http4k.connect.module")
}

dependencies {
    api(project(":http4k-realtime-core"))
    api(project(":http4k-format-jackson"))
    api(project(":http4k-api-jsonrpc"))
    api(project(":http4k-api-jsonschema"))
    api(project(":http4k-connect-core"))
    api("dev.forkhandles:values4k")
    api("dev.forkhandles:result4k")

    testImplementation(project(":http4k-server-helidon"))

    testImplementation(project(":http4k-format-moshi"))
}

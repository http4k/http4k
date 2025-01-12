import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k Pro Incubator code"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.pro")
    id("org.http4k.connect.module")
}

dependencies {
    api(project(":http4k-server-helidon"))
    api(project(":http4k-realtime-core"))
    api(project(":http4k-api-jsonrpc"))
    testImplementation(project(":http4k-format-moshi"))
}

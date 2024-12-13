import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k YAML support using Moshi and SnakeYaml as an underlying engine"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-realtime-core"))
    api(project(":http4k-format-moshi"))
    api("org.yaml:snakeyaml:_")

    testImplementation(project(":http4k-core"))
    testImplementation("dev.forkhandles:values4k:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
}

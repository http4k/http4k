import org.http4k.internal.ModuleLicense.Apache2

description = "http4k YAML support using Jackson as an underlying engine"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(project(":http4k-format-jackson"))
    api(platform(libs.jackson.bom))
    api(libs.jackson.dataformat.yaml)
    testImplementation(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(testFixtures(project(":http4k-format-jackson")))
}

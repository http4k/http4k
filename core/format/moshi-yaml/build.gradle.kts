

description = "http4k YAML support using Moshi and SnakeYaml as an underlying engine"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-realtime-core"))
    api(project(":http4k-format-moshi"))
    api(libs.snakeyaml)

    testImplementation(project(":http4k-core"))
    testImplementation(libs.values4k)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
}

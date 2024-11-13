description = "Http4k XML support using GSON as an underlying engine"

plugins {
    id("org.http4k.module")
}

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-format-gson"))
    api("org.json:json:_")
    testImplementation(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
}

description = "Http4k YAML support using Jackson as an underlying engine"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
    id("org.http4k.code-coverage")
}

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(project(":http4k-format-jackson"))
    api(platform("com.fasterxml.jackson:jackson-bom:_"))
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    testImplementation(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(testFixtures(project(":http4k-format-jackson")))
}

description = "Http4k CSV support using Jackson as an underlying engine"

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(project(":http4k-format-jackson"))
    api(platform("com.fasterxml.jackson:jackson-bom:_"))
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
    testImplementation(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(testFixtures(project(":http4k-format-jackson")))
}

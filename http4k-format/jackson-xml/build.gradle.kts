description = "Http4k XML support using Jackson as an underlying engine"

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-format-jackson"))
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:_")
    testImplementation(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
}

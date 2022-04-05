description = "Http4k CSV support using Jackson as an underlying engine"

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(project(":http4k-format-jackson"))
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:_")
    testImplementation(project(":http4k-core"))
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
    testImplementation(project(path = ":http4k-format-core", configuration ="testArtifacts"))
    testImplementation(project(path = ":http4k-format-jackson", configuration ="testArtifacts"))
}

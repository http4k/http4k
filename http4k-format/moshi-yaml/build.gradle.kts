description = "Http4k YAML support using Moshi and SnakeYaml as an underlying engine"

dependencies {
    api(project(":http4k-realtime-core"))
    api(project(":http4k-format-moshi"))
    api("org.yaml:snakeyaml:_")

    testImplementation(project(":http4k-core"))
    testImplementation("dev.forkhandles:values4k:_")
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
    testImplementation(project(path = ":http4k-format-core", configuration ="testArtifacts"))
}

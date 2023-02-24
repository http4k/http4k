description = "Http4k GSON JSON support"

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api("com.google.code.gson:gson:_")
    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-contract"))
    testImplementation(project(":http4k-jsonrpc"))
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-contract", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-jsonrpc", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-format-core", configuration = "testArtifacts"))
}

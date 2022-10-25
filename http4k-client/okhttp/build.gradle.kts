description = "HTTP Client built on top of okhttp"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api(Square.okHttp3)
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
    testImplementation(project(path = ":http4k-server-undertow"))
    testImplementation(project(path = ":http4k-realtime-core", configuration ="testArtifacts"))
}
